package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.RequestedSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.PublishedCatalogItem;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionCommercialSnapshotPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.ContractPriceTier;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.EmployeeRef;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddSubscriptionItemService - abrir una linea")
class AddSubscriptionItemServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long TARIFA = 3L;
    private static final Long ARTICULO = 100L;
    private static final Long USUARIO_EXTRA = 200L;
    private static final String LLAVE = "req-abc-123";
    /**
     * Fecha efectiva del cambio, <b>dentro del periodo en curso</b> del contrato de
     * prueba (1..31 de enero de 2026).
     *
     * <p>
     * <b>Antes era el 1 de mayo, y ese fixture afirmaba un defecto.</b> Un cambio
     * efectivo fuera del periodo en curso solo puede darse si el periodo se quedo
     * congelado -que es justo lo que pasaba, porque nada llamaba a
     * {@code Subscription.renewPeriod}-. El prorrateo salia cero y se guardaba en
     * el otrosi como si fuera un importe: firmado, inmutable y con toda la pinta de
     * estar bien. Hoy {@code ProrationCalculator} rechaza el prorrateo de cero
     * dias, asi que la fecha efectiva vuelve a caer donde la produce el negocio.
     */
    private static final LocalDate ENERO_17 = LocalDate.of(2026, 1, 17);
    private static final LocalDate ENERO_24 = LocalDate.of(2026, 1, 24);
    private static final LocalDate DICIEMBRE_31 = LocalDate.of(2026, 12, 31);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionItemRepository itemRepository;
    @Mock
    private SubscriptionAmendmentRepository amendmentRepository;
    @Mock
    private SubscriptionCommercialSnapshotPort commercialSnapshotPort;
    @Mock
    private SubscriptionItemCompositionPort compositionPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private SystemUserValidationPort systemUserValidationPort;
    @Mock
    private SubscriptionNumberPort subscriptionNumberPort;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;
    @Mock
    private SubscriptionAuditPort audit;

    @InjectMocks
    private AddSubscriptionItemService service;

    private static Subscription contrato() {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, TARIFA,
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, LocalDate.of(2026, 1, 1), null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null, 0, null, true,
                null, null, 0L, true);
    }

    /** El modulo tal y como lo publica la tarifa: 179.000, sin escalones. */
    private static PublishedCatalogItem moduloPublicado() {
        return new PublishedCatalogItem(ARTICULO, "VET", "Veterinaria", SubscriptionItemType.MODULE,
                null, List.of(new ContractPriceTier(1, null, 2, TaxTreatment.TAXED,
                        new BigDecimal("179000.00"), new BigDecimal("19.00"))));
    }

    /** La escalera de D-66, en unidades extra: 1 a 8 a 12.000, de la 9 a 9.000. */
    private static PublishedCatalogItem usuarioExtraPublicado() {
        return new PublishedCatalogItem(USUARIO_EXTRA, "EXTRA_USER", "Usuario adicional",
                SubscriptionItemType.CAPACITY, "USER",
                List.of(new ContractPriceTier(1, 8, 2, TaxTreatment.TAXED,
                        new BigDecimal("12000.00"), new BigDecimal("19.00")),
                        new ContractPriceTier(9, null, 0, TaxTreatment.TAXED,
                                new BigDecimal("9000.00"), new BigDecimal("19.00"))));
    }

    private static RequestedSubscriptionItemCommand seleccion(LocalDate from, LocalDate to) {
        return new RequestedSubscriptionItemCommand(ARTICULO, 5, from, to);
    }

    private static AddSubscriptionItemCommand comando(RequestedSubscriptionItemCommand linea) {
        return new AddSubscriptionItemCommand(CONTRATO, EMPRESA, LLAVE, ENERO_17,
                "Contrato veterinaria", 55L, null, null, linea);
    }

    private static SubscriptionAmendment otrosiGuardado() {
        return new SubscriptionAmendment(900L, EMPRESA, CONTRATO, "AMD-2026-0001",
                AmendmentType.ADD_ITEM, ENERO_17, null, 55L, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, LLAVE, null);
    }

    private static SubscriptionItem tramoExistente(LocalDate from, LocalDate to) {
        return SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "VET", "Veterinaria",
                SubscriptionItemType.MODULE, null, 1, null, 2, TaxTreatment.TAXED, 1,
                new BigDecimal("179000.00"), BigDecimal.ZERO, BigDecimal.ZERO, false,
                BigDecimal.ZERO, new EffectivePeriod(from, to), ItemOrigin.ADDON, null);
    }

    @BeforeEach
    void elConsecutivoLoReservaElServidor() {
        // El numero del otrosi ya no viaja en el command: lo reserva el puerto dentro
        // de la transaccion. Leniente porque los caminos que rechazan antes —solape,
        // reintento idempotente, empleado ajeno— no llegan a pedirlo.
        lenient().when(subscriptionNumberPort.nextAmendmentNumber(anyInt()))
                .thenReturn("AMD-2026-00001");
    }

    private void caminoFeliz(PublishedCatalogItem publicado) {
        when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(contrato()));
        when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
        when(commercialSnapshotPort.findPublishedItem(eqTarifa(), any(), anyLong(), anyInt(),
                any())).thenReturn(Optional.of(publicado));
        when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());
        when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
        when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static Long eqTarifa() {
        return org.mockito.ArgumentMatchers.eq(TARIFA);
    }

    @Nested
    @DisplayName("R-QUOTE-02 — el precio lo resuelve el servidor, no el cuerpo")
    class ElPrecioLoResuelveElServidor {

        @Test
        @DisplayName("una peticion de ampliacion con unit_amount 0 es ignorada y se usa el precio "
                + "de la lista vigente")
        void una_peticion_de_ampliacion_con_unit_amount_0_es_ignorada_y_se_usa_el_precio_de_la_lista_vigente() {
            caminoFeliz(moduloPublicado());

            // La peticion ya NO PUEDE llevar importe: RequestedSubscriptionItemCommand solo
            // declara articulo, cantidad y fechas. Antes traia unitAmount con
            // @PositiveOrZero —el cero explicito— y el servicio lo copiaba a la fila.
            service.execute(comando(seleccion(ENERO_17, null)));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository).save(captor.capture());
            SubscriptionItem guardada = captor.getValue();
            assertThat(guardada.getUnitAmount()).isEqualByComparingTo("179000.00");
            assertThat(guardada.getUnitAmount()).isNotEqualByComparingTo("0.00");
            assertThat(guardada.getItemName()).isEqualTo("Veterinaria");
            assertThat(guardada.getItemCode()).isEqualTo("VET");
            assertThat(guardada.getTaxRate()).isEqualByComparingTo("19.00");
            assertThat(guardada.getTaxTreatment()).isEqualTo(TaxTreatment.TAXED);
        }

        @Test
        @DisplayName("una peticion de ampliacion con included_quantity 9999 no llega al techo del "
                + "contador")
        void una_peticion_de_ampliacion_con_included_quantity_9999_no_llega_al_techo_del_contador() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository).save(captor.capture());
            // Lo incluido sale de la tarifa (2), no de un numero del cuerpo. El techo que
            // acaba en company_capacities es included_quantity + quantity: con 9999 en el
            // cuerpo, la clinica se autoconcedia diez mil unidades sin pagar ninguna.
            assertThat(captor.getValue().getIncludedQuantity()).isEqualTo(2);
            assertThat(captor.getValue().getIncludedQuantity()).isNotEqualTo(9999);
        }

        @Test
        @DisplayName("el nombre y el tipo tampoco se aceptan del cuerpo: salen del catalogo")
        void el_nombre_y_el_tipo_salen_del_catalogo() {
            caminoFeliz(usuarioExtraPublicado());

            service.execute(comando(
                    new RequestedSubscriptionItemCommand(USUARIO_EXTRA, 3, ENERO_17, null)));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository).save(captor.capture());
            assertThat(captor.getValue().getItemName()).isEqualTo("Usuario adicional");
            assertThat(captor.getValue().getItemType()).isEqualTo(SubscriptionItemType.CAPACITY);
            assertThat(captor.getValue().getCapacityUnit()).isEqualTo("USER");
        }

        @Test
        @DisplayName("resuelve contra la tarifa DEL CONTRATO y en la fecha en que la linea empieza "
                + "a servir")
        void resuelve_contra_la_tarifa_del_contrato() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_24, null)));

            verify(commercialSnapshotPort).findPublishedItem(TARIFA, BillingCycle.MONTHLY, ARTICULO,
                    5, ENERO_24);
        }

        @Test
        @DisplayName("sin precio publicado y vigente no se abre nada: ni otrosi, ni linea, ni "
                + "evento")
        void sin_precio_publicado_no_se_abre_nada() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(commercialSnapshotPort.findPublishedItem(anyLong(), any(), anyLong(), anyInt(),
                    any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(seleccion(ENERO_17, null))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No published price");

            verify(amendmentRepository, never()).save(any());
            verify(itemRepository, never()).save(any());
            verify(subscriptionChangedPort, never()).subscriptionChanged(any());
            // Sin tarifa vigente no hay importe que auditar: el rechazo no deja rastro.
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("el prorrateo se calcula sobre el precio de la tarifa, no sobre un importe del "
                + "cuerpo")
        void el_prorrateo_se_calcula_sobre_el_precio_de_la_tarifa() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            ArgumentCaptor<SubscriptionAmendment> captor = ArgumentCaptor
                    .forClass(SubscriptionAmendment.class);
            verify(amendmentRepository).save(captor.capture());
            // 5 contratadas menos 2 incluidas = 3 a 179.000.
            assertThat(captor.getValue().getMonthlyDeltaAmount()).isEqualByComparingTo("537000.00");
        }
    }

    @Nested
    @DisplayName("D-66 — la ampliacion escalonada se parte por tramos")
    class TramosAcumulativos {

        @Test
        @DisplayName("ampliar a quince usuarios abre dos lineas de tramo que suman 141000")
        void ampliar_a_quince_usuarios_abre_dos_lineas_de_tramo_que_suman_141000() {
            caminoFeliz(usuarioExtraPublicado());

            service.execute(comando(
                    new RequestedSubscriptionItemCommand(USUARIO_EXTRA, 15, ENERO_17, null)));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository, Mockito.times(2)).save(captor.capture());
            List<SubscriptionItem> lineas = captor.getAllValues();
            assertThat(lineas.get(0).getTierMin()).isEqualTo(1);
            assertThat(lineas.get(0).getTierMax()).isEqualTo(8);
            assertThat(lineas.get(0).billableQuantity()).isEqualTo(8);
            assertThat(lineas.get(1).getTierMin()).isEqualTo(9);
            assertThat(lineas.get(1).getTierMax()).isNull();
            assertThat(lineas.get(1).billableQuantity()).isEqualTo(5);

            BigDecimal cuota = lineas.stream().map(SubscriptionItem::recurringSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(cuota).isEqualByComparingTo("141000.00");
            assertThat(cuota).isNotEqualByComparingTo("117000.00");
        }

        @Test
        @DisplayName("el otrosi declara lo que sube la cuota sumando TODOS los tramos")
        void el_otrosi_declara_lo_que_sube_la_cuota_sumando_todos_los_tramos() {
            caminoFeliz(usuarioExtraPublicado());

            service.execute(comando(
                    new RequestedSubscriptionItemCommand(USUARIO_EXTRA, 15, ENERO_17, null)));

            ArgumentCaptor<SubscriptionAmendment> captor = ArgumentCaptor
                    .forClass(SubscriptionAmendment.class);
            verify(amendmentRepository).save(captor.capture());
            assertThat(captor.getValue().getMonthlyDeltaAmount()).isEqualByComparingTo("141000.00");
        }

        @Test
        @DisplayName("el tramo se escribe en la linea: deja de ser columna muerta")
        void el_tramo_se_escribe_en_la_linea() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository).save(captor.capture());
            assertThat(captor.getValue().getTierMin()).isEqualTo(1);
            assertThat(captor.getValue().getTierMax()).isNull();
        }
    }

    @Nested
    @DisplayName("D-76 — la composicion se congela al firmar")
    class ComposicionCongelada {

        @Test
        @DisplayName("congela la composicion de cada linea que abre")
        void congela_la_composicion_de_cada_linea_que_abre() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            verify(compositionPort).freeze(EMPRESA, null, ARTICULO);
        }
    }

    @Nested
    @DisplayName("Solapes")
    class Solapes {

        @Test
        @DisplayName("rechaza el tramo que se pisa con uno ya existente")
        void rechazaElSolape() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(commercialSnapshotPort.findPublishedItem(anyLong(), any(), anyLong(), anyInt(),
                    any())).thenReturn(Optional.of(moduloPublicado()));
            when(itemRepository.findOverlapping(EMPRESA, CONTRATO, ARTICULO, ENERO_17, DICIEMBRE_31,
                    null)).thenReturn(List.of(tramoExistente(LocalDate.of(2026, 1, 1), ENERO_24)));

            assertThatThrownBy(() -> service.execute(comando(seleccion(ENERO_17, DICIEMBRE_31))))
                    .isInstanceOf(SubscriptionItemOverlapException.class);

            // Y no deja rastro: ni otrosi, ni linea, ni evento, ni auditoria.
            verify(amendmentRepository, never()).save(any());
            verify(itemRepository, never()).save(any());
            verify(subscriptionChangedPort, never()).subscriptionChanged(any());
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("bloquea el contrato ANTES de preguntar por el solape")
        void bloqueaAntesDeComprobar() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            // Sin el bloqueo la comprobacion es una carrera: dos transacciones
            // concurrentes pasan las dos y las dos insertan.
            InOrder orden = Mockito.inOrder(subscriptionRepository, itemRepository);
            orden.verify(subscriptionRepository).lockByIdAndCompanyId(CONTRATO, EMPRESA);
            orden.verify(itemRepository).findOverlapping(anyLong(), anyLong(), anyLong(), any(),
                    any(), any());
        }

        @Test
        @DisplayName("acepta el tramo que empieza donde acaba el anterior")
        void aceptaElTramoConsecutivo() {
            caminoFeliz(moduloPublicado());

            SubscriptionItemDto resultado = service.execute(comando(seleccion(ENERO_24, null)));

            assertThat(resultado.effectiveFrom()).isEqualTo(ENERO_24);
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("el segundo clic devuelve la linea del primero y no crea otra")
        void segundoClicNoDuplica() {
            SubscriptionItem yaCreada = new SubscriptionItem(500L, EMPRESA, CONTRATO, ARTICULO,
                    "VET", "Veterinaria", SubscriptionItemType.MODULE, null, 1, null, 2,
                    TaxTreatment.TAXED, 5, new BigDecimal("179000.00"), BigDecimal.ZERO,
                    BigDecimal.ZERO, false, BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_17),
                    ItemOrigin.ADDON, 900L, null, null, 0L, true);
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.of(otrosiGuardado()));
            when(itemRepository.findAllByCreatedAmendmentIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(List.of(yaCreada));

            SubscriptionItemDto resultado = service.execute(comando(seleccion(ENERO_17, null)));

            assertThat(resultado.id()).isEqualTo(500L);
            // Ni siquiera llega a bloquear el contrato: se busca antes de insertar.
            verify(subscriptionRepository, never()).lockByIdAndCompanyId(anyLong(), anyLong());
            verify(amendmentRepository, never()).save(any());
            verify(itemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Congelacion y efectos")
    class CongelacionYEfectos {

        @Test
        @DisplayName("anuncia que el contrato cambio, para que el recalculo pueda dispararse")
        void anunciaElCambio() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(
                    EMPRESA, CONTRATO, SubscriptionChangeKind.ITEM_ADDED, ENERO_17));
        }

        @Test
        @DisplayName("R14: el empleado que firma tiene que ser de la empresa del contrato")
        void empleadoDeOtraEmpresa() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(seleccion(ENERO_17, null))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Employee");

            verify(amendmentRepository, never()).save(any());
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("un contrato de otra empresa no existe para el caller")
        void contratoAjeno() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(seleccion(ENERO_17, null))))
                    .isInstanceOf(SubscriptionNotFoundException.class);

            verifyNoInteractions(audit);
        }
    }

    @Nested
    @DisplayName("Auditoria — issue #607: el importe que se le enseño al cliente antes de confirmar")
    class Auditoria {

        @Test
        @DisplayName("emite itemAdded con el contrato, el articulo, la cantidad y la cuota que sube "
                + "-nunca con el prorrateo del primer periodo-")
        void emite_itemAdded_con_los_valores_correctos() {
            caminoFeliz(moduloPublicado());

            service.execute(comando(seleccion(ENERO_17, null)));

            ArgumentCaptor<BigDecimal> monto = ArgumentCaptor.forClass(BigDecimal.class);
            verify(audit).itemAdded(eq(CONTRATO), isNull(), eq(ARTICULO), eq(5), monto.capture(),
                    eq(900L));
            // 5 contratadas menos 2 incluidas = 3 a 179.000: la misma cuenta que ya prueba
            // el otrosi. Lo que esta asercion separa son DOS numeros distintos que el
            // servicio podria confundir: el delta de la cuota recurrente (537.000, que es
            // lo que se audita) y el prorrateo de los dias que quedan del periodo en curso
            // -del 17 al 31 de enero, 15 de 31 dias, es decir 259.838,71-. Antes esta
            // prueba se apoyaba en que el prorrateo saliera CERO porque la fecha efectiva
            // caia fuera del periodo; eso afirmaba el defecto del periodo congelado. Ahora
            // los dos numeros son distintos y no nulos, que es una separacion mas fuerte.
            assertThat(monto.getValue()).isEqualByComparingTo("537000.00");
            assertThat(monto.getValue()).isNotEqualByComparingTo(new BigDecimal("259838.71"));
        }

        @Test
        @DisplayName("una ampliacion escalonada audita una sola vez, con la cantidad total y el "
                + "delta agregado de todos los tramos")
        void audita_una_sola_vez_con_el_delta_agregado_de_todos_los_tramos() {
            caminoFeliz(usuarioExtraPublicado());

            service.execute(comando(
                    new RequestedSubscriptionItemCommand(USUARIO_EXTRA, 15, ENERO_17, null)));

            // Dos lineas de tramo (8 a 12.000, 5 a 9.000) y UNA sola llamada de auditoria,
            // con la cantidad total pedida y la suma de los dos tramos: 96.000 + 45.000.
            verify(audit).itemAdded(eq(CONTRATO), isNull(), eq(USUARIO_EXTRA), eq(15),
                    eq(new BigDecimal("141000.00")), eq(900L));
        }
    }
}
