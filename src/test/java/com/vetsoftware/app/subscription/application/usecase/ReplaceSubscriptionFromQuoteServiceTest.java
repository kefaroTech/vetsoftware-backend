package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.ReplaceSubscriptionFromQuoteCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionLifecycleMetrics;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionQuoteSnapshotPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.StructuralMinimumNotCarriedException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DC-2: aceptar una cotizacion sustituye el contrato vigente por el que esa
 * oferta describe.
 *
 * <p>
 * Estos tests estan escritos contra el <b>javadoc de
 * {@code ReplaceSubscriptionFromQuoteUseCase}</b> —«cerrar y abrir tienen que
 * ocurrir en la misma transaccion», «el minimo estructural viaja», «nace sin
 * cobrar»— y no contra la implementacion. Cada uno de los tres bloques de abajo
 * se pone <b>rojo</b> si se revierte la logica que lo motiva, y eso es lo unico
 * que los hace valer algo:
 *
 * <ul>
 * <li><b>Minimo estructural</b>: si {@code withStructuralMinimum} devolviera
 * las lineas de la oferta tal cual, la empresa firmaria un contrato sin sedes
 * ni usuarios. Los tests miran las lineas que de verdad se mandan a
 * firmar.</li>
 * <li><b>Orden y atomicidad</b>: si alguien invirtiera el orden —abrir y luego
 * cerrar— produccion reventaria contra {@code uq_subscriptions_active_company};
 * el {@code InOrder} lo caza sin base de datos. Y si alguien envolviera el alta
 * en un {@code try/catch} «para no perder la aceptacion», el test de
 * propagacion se pone rojo.</li>
 * <li><b>Doble conversion</b>: si se quitara la guarda de reintento, la segunda
 * llamada cancelaria el contrato recien creado y abriria un tercero.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que estos tests NO pueden probar, y hay que decirlo:</b> que la
 * transaccion revierta de verdad. Aqui no hay transaccion —son mocks—, asi que
 * lo que se afirma es la condicion <em>necesaria</em> (que la excepcion se
 * propague en vez de tragarse) y no la suficiente. La atomicidad real, y la
 * carrera de dos aceptaciones simultaneas de la misma cotizacion, solo las
 * demuestra una rodaja {@code @DataJpaTest} con MySQL de verdad y el unico
 * {@code uq_subscriptions_quote} aplicado. Esa rodaja NO esta escrita.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReplaceSubscriptionFromQuoteService - la cotizacion aceptada sustituye al contrato")
class ReplaceSubscriptionFromQuoteServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long COTIZACION = 77L;
    private static final Long TARIFA = 9L;
    private static final Long CONTRATO_VIEJO = 500L;
    private static final LocalDate HOY = LocalDate.of(2026, 8, 30);

    private static final Clock RELOJ = Clock.fixed(
            HOY.atTime(10, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
            ZoneId.of("America/Bogota"));

    @Mock
    private SubscriptionQuoteSnapshotPort quoteSnapshotPort;

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private SubscriptionItemRepository itemRepository;

    @Mock
    private SubscriptionStatusHistoryRepository historyRepository;

    @Mock
    private SubscriptionAuditPort audit;

    @Mock
    private SubscriptionLifecycleMetrics metrics;

    @Mock
    private PlatformCatalogPort platformCatalogPort;

    @Mock
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    private ReplaceSubscriptionFromQuoteService service() {
        return new ReplaceSubscriptionFromQuoteService(quoteSnapshotPort, repository,
                itemRepository, historyRepository, audit, metrics, platformCatalogPort,
                createSubscriptionUseCase, RELOJ);
    }

    // ---------------------------------------------------------------- fixtures

    private static SubscriptionQuoteSnapshot ofertaAceptada(SubscriptionItemSnapshot... lineas) {
        return new SubscriptionQuoteSnapshot(COTIZACION, EMPRESA, TARIFA, BillingCycle.MONTHLY,
                true, "duena@clinica.com", List.of(lineas));
    }

    /** Un modulo: no concede ninguna capacidad. */
    private static SubscriptionItemSnapshot moduloAgenda() {
        return new SubscriptionItemSnapshot(11L, "MOD_AGENDA", "Agenda",
                SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                new BigDecimal("50000.00"), new BigDecimal("19.00"));
    }

    private static SubscriptionItemSnapshot capacidad(String unidad, Long catalogItemId) {
        return new SubscriptionItemSnapshot(catalogItemId, "CAP_" + unidad, "Capacidad " + unidad,
                SubscriptionItemType.CAPACITY, unidad, 1, TaxTreatment.TAXED, 3,
                new BigDecimal("12000.00"), new BigDecimal("19.00"));
    }

    private static Subscription contratoVigente(Long quoteId) {
        return new Subscription(CONTRATO_VIEJO, "SUB-2026-0001", EMPRESA, quoteId, TARIFA,
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, HOY.minusMonths(3), null,
                HOY.minusDays(10), HOY.plusDays(20), HOY.plusDays(21), null, 5, null, true, null,
                LocalDateTime.now(RELOJ), 1L, true);
    }

    /**
     * Una linea de capacidad del contrato que se va a cerrar, con valores
     * congelados que los tests reconocen: si el arrastre recotizara contra el
     * catalogo, estos numeros cambiarian.
     */
    private static SubscriptionItem lineaDeCapacidad(String unidad, Long catalogItemId, int tierMin,
            Integer tierMax, int cantidad, String precio) {
        return new SubscriptionItem(catalogItemId + tierMin, EMPRESA, CONTRATO_VIEJO, catalogItemId,
                "CAP_" + unidad, "Capacidad " + unidad, SubscriptionItemType.CAPACITY, unidad,
                tierMin, tierMax, 2, TaxTreatment.TAXED, cantidad, new BigDecimal(precio),
                BigDecimal.ZERO, BigDecimal.ZERO, false, new BigDecimal("19.00"),
                EffectivePeriod.openFrom(HOY.minusMonths(3)), ItemOrigin.INITIAL, null, null,
                LocalDateTime.now(RELOJ), 1L, true);
    }

    private void catalogoSinPrueba() {
        when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                .thenReturn(Optional.of(new InitialContractTemplate(TARIFA, 1L, "CORE", "Nucleo",
                        SubscriptionItemType.MODULE, null, 0, 1, new BigDecimal("100000.00"),
                        new BigDecimal("19.00"), TaxTreatment.TAXED, 5, 0)));
    }

    private void devuelveContratoNuevo() {
        when(createSubscriptionUseCase.execute(any())).thenAnswer(invocation -> {
            CreateSubscriptionCommand c = invocation.getArgument(0);
            return SubscriptionDto.from(Subscription.create("SUB-2026-0002", c.companyId(),
                    c.quoteId(), c.priceListId(), c.billingCycle(), c.status(), c.startDate(),
                    c.trialEndDate(), c.currentPeriodStart(), c.currentPeriodEnd(),
                    c.nextBillingDate(), c.commitmentEndDate(), 5, true));
        });
    }

    private void guardaElCierre() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateSubscriptionCommand comandoFirmado() {
        ArgumentCaptor<CreateSubscriptionCommand> captor = ArgumentCaptor
                .forClass(CreateSubscriptionCommand.class);
        verify(createSubscriptionUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private ReplaceSubscriptionFromQuoteCommand comando() {
        return new ReplaceSubscriptionFromQuoteCommand(COTIZACION, EMPRESA);
    }

    // ------------------------------------------------------- minimo estructural

    @Nested
    @DisplayName("El minimo estructural viaja al contrato nuevo")
    class MinimoEstructural {

        @Test
        @DisplayName("una oferta de solo modulos arrastra sede y usuarios del contrato que cierra")
        void una_oferta_de_solo_modulos_arrastra_sede_y_usuarios() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda())));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            when(itemRepository.findAllCurrentOn(CONTRATO_VIEJO, EMPRESA, HOY))
                    .thenReturn(List.of(lineaDeCapacidad("BRANCH", 20L, 1, null, 2, "30000.00"),
                            lineaDeCapacidad("USER", 21L, 1, null, 5, "12000.00")));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            List<SubscriptionItemLineCommand> lineas = comandoFirmado().items();
            assertThat(lineas).extracting(SubscriptionItemLineCommand::capacityUnit)
                    .containsExactly(null, "BRANCH", "USER");
        }

        @Test
        @DisplayName("arrastra los valores CONGELADOS, no la tarifa de hoy")
        void arrastra_los_valores_congelados() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda())));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            when(itemRepository.findAllCurrentOn(CONTRATO_VIEJO, EMPRESA, HOY))
                    .thenReturn(List.of(lineaDeCapacidad("BRANCH", 20L, 1, null, 2, "30000.00"),
                            lineaDeCapacidad("USER", 21L, 1, null, 5, "12000.00")));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            SubscriptionItemLineCommand sede = comandoFirmado().items().stream()
                    .filter(l -> "BRANCH".equals(l.capacityUnit())).findFirst().orElseThrow();
            assertThat(sede.unitAmount()).isEqualByComparingTo("30000.00");
            assertThat(sede.quantity()).isEqualTo(2);
            assertThat(sede.includedQuantity()).isEqualTo(2);
            assertThat(sede.effectiveFrom()).isEqualTo(HOY);
            assertThat(sede.effectiveTo()).isNull();
        }

        @Test
        @DisplayName("arrastra TODOS los tramos de un eje escalonado, no solo el primero")
        void arrastra_todos_los_tramos() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda())));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            when(itemRepository.findAllCurrentOn(CONTRATO_VIEJO, EMPRESA, HOY))
                    .thenReturn(List.of(lineaDeCapacidad("BRANCH", 20L, 1, null, 2, "30000.00"),
                            // USER escalonado: dos tramos del MISMO articulo
                            lineaDeCapacidad("USER", 21L, 9, null, 5, "9000.00"),
                            lineaDeCapacidad("USER", 21L, 1, 8, 8, "12000.00")));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            assertThat(comandoFirmado().items()).filteredOn(l -> "USER".equals(l.capacityUnit()))
                    // ordenados por tramo, y con la cantidad de CADA tramo intacta
                    .extracting(SubscriptionItemLineCommand::tierMin,
                            SubscriptionItemLineCommand::quantity)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(1, 8),
                            org.assertj.core.groups.Tuple.tuple(9, 5));
        }

        @Test
        @DisplayName("si la oferta ya trae el minimo no arrastra nada y no lee el contrato viejo")
        void si_la_oferta_ya_trae_el_minimo_no_arrastra_nada() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            assertThat(comandoFirmado().items()).hasSize(3);
            // Duplicar una capacidad que la oferta ya concede abriria dos lineas del
            // mismo articulo pisandose (R7): el contrato facturaria ese eje dos veces.
            verifyNoInteractions(itemRepository);
        }

        @Test
        @DisplayName("si ni la oferta ni el contrato viejo conceden el minimo, no se firma nada")
        void si_nadie_concede_el_minimo_no_se_firma_nada() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda())));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            when(itemRepository.findAllCurrentOn(CONTRATO_VIEJO, EMPRESA, HOY))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service().execute(comando()))
                    .isInstanceOf(StructuralMinimumNotCarriedException.class)
                    // las DOS unidades en el mismo mensaje, no la primera y a reintentar
                    .hasMessageContaining("BRANCH").hasMessageContaining("USER");

            // Y sobre todo: el contrato viejo sigue vivo. Fallar despues de cerrarlo
            // dejaria a la empresa sin ninguno.
            verify(repository, never()).save(any());
            verifyNoInteractions(createSubscriptionUseCase);
        }
    }

    // -------------------------------------------------------- orden y atomicidad

    @Nested
    @DisplayName("Cerrar y abrir, en ese orden y en la misma transaccion")
    class OrdenYAtomicidad {

        @Test
        @DisplayName("cierra el contrato vigente ANTES de abrir el nuevo")
        void cierra_antes_de_abrir() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            // uq_subscriptions_active_company no admite dos vigentes: invertir este
            // orden revienta en produccion y este InOrder lo caza sin base de datos.
            InOrder orden = inOrder(repository, createSubscriptionUseCase);
            orden.verify(repository).save(any());
            orden.verify(createSubscriptionUseCase).execute(any());
        }

        @Test
        @DisplayName("el cierre queda CANCELLED y anotado en la bitacora")
        void el_cierre_queda_cancelado_y_anotado() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            ArgumentCaptor<Subscription> cerrado = ArgumentCaptor.forClass(Subscription.class);
            verify(repository).save(cerrado.capture());
            assertThat(cerrado.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(cerrado.getValue().isCurrent()).isFalse();

            ArgumentCaptor<SubscriptionStatusChange> fila = ArgumentCaptor
                    .forClass(SubscriptionStatusChange.class);
            verify(historyRepository).append(fila.capture());
            // El rotulo correcto: ni MANUAL ni CANCELLATION_EFFECTIVE. Ver el javadoc
            // de SubscriptionStatusChangeReason.REPLACED_BY_NEW_CONTRACT.
            assertThat(fila.getValue().getReason()).isEqualTo("replaced_by_new_contract");
            assertThat(fila.getValue().getToStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("si el alta del nuevo falla, el fallo se propaga y no se traga")
        void si_el_alta_falla_el_fallo_se_propaga() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            catalogoSinPrueba();
            guardaElCierre();
            when(createSubscriptionUseCase.execute(any()))
                    .thenThrow(new IllegalStateException("el numero de contrato colisiono"));

            // Este es el borde que mas importa: el cierre ya se escribio. Si alguien
            // capturase aqui «para no perder la aceptacion», la empresa se quedaria SIN
            // NINGUN contrato vigente -sin company_entitlements, dentro del sistema y
            // sin poder hacer nada-. Propagar es lo que hace que la transaccion
            // revierta tambien el cierre.
            assertThatThrownBy(() -> service().execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("el numero de contrato colisiono");
        }
    }

    // ---------------------------------------------------------- doble conversion

    @Nested
    @DisplayName("Una cotizacion, un contrato")
    class DobleConversion {

        @Test
        @DisplayName("un reintento devuelve el contrato que ya nacio de esa cotizacion")
        void un_reintento_devuelve_el_contrato_que_ya_existe() {
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(COTIZACION)));
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));

            SubscriptionDto resultado = service().execute(comando());

            assertThat(resultado.id()).isEqualTo(CONTRATO_VIEJO);
            assertThat(resultado.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            // Sin la guarda, esta segunda llamada cancelaria el contrato recien creado
            // y abriria un tercero desde el mismo papel.
            verify(repository, never()).save(any());
            verifyNoInteractions(createSubscriptionUseCase, historyRepository, audit, metrics);
        }

        @Test
        @DisplayName("una oferta que no esta ACEPTADA no firma nada")
        void una_oferta_no_aceptada_no_firma_nada() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(new SubscriptionQuoteSnapshot(COTIZACION, EMPRESA,
                            TARIFA, BillingCycle.MONTHLY, false, null, List.of(moduloAgenda()))));

            assertThatThrownBy(() -> service().execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Quote must be ACCEPTED");

            verify(repository, never()).save(any());
            verifyNoInteractions(createSubscriptionUseCase);
        }
    }

    // ------------------------------------------------------------ nace sin cobrar

    @Nested
    @DisplayName("El contrato nace sin cobrar")
    class NaceSinCobrar {

        @Test
        @DisplayName("con prueba por defecto del catalogo nace TRIALING y con su ventana")
        void con_prueba_por_defecto_nace_trialing() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY)).thenReturn(
                    Optional.of(new InitialContractTemplate(TARIFA, 1L, "CORE", "Nucleo",
                            SubscriptionItemType.MODULE, null, 0, 1, new BigDecimal("100000.00"),
                            new BigDecimal("19.00"), TaxTreatment.TAXED, 5, 14)));
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            CreateSubscriptionCommand firmado = comandoFirmado();
            assertThat(firmado.status()).isEqualTo(SubscriptionStatus.TRIALING);
            assertThat(firmado.trialEndDate()).isEqualTo(HOY.plusDays(14));
            // Los dias de gracia los resuelve CreateSubscriptionService desde
            // platform_billing_config: duplicar el valor por defecto aqui es como
            // divergieron los dos caminos de alta (#467).
            assertThat(firmado.graceDays()).isNull();
        }

        @Test
        @DisplayName("sin prueba en el catalogo no se inventa una ventana")
        void sin_prueba_no_se_inventa_ventana() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            CreateSubscriptionCommand firmado = comandoFirmado();
            assertThat(firmado.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(firmado.trialEndDate()).isNull();
        }

        @Test
        @DisplayName("el contrato apunta a la cotizacion que lo origino")
        void el_contrato_apunta_a_su_cotizacion() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(ofertaAceptada(moduloAgenda(), capacidad("BRANCH", 20L),
                            capacidad("USER", 21L))));
            when(repository.findCurrentByCompanyId(EMPRESA))
                    .thenReturn(Optional.of(contratoVigente(null)));
            catalogoSinPrueba();
            guardaElCierre();
            devuelveContratoNuevo();

            service().execute(comando());

            CreateSubscriptionCommand firmado = comandoFirmado();
            // Sin esto no hay forma de saber de que papel salio el contrato, y el unico
            // uq_subscriptions_quote que cierra la doble conversion no tendria columna
            // sobre la que actuar.
            assertThat(firmado.quoteId()).isEqualTo(COTIZACION);
            assertThat(firmado.priceListId()).isEqualTo(TARIFA);
            // El actor de la bitacora es quien acepto, no un SYSTEM generico.
            assertThat(firmado.actor()).isEqualTo("duena@clinica.com");
        }
    }
}
