package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.InitialCapacityTemplate;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateInitialSubscriptionService - toda empresa nace con un contrato")
class CreateInitialSubscriptionServiceTest {

    private static final Long EMPRESA = 42L;
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);

    @Mock
    private PlatformCatalogPort platformCatalogPort;
    @Mock
    private CreateSubscriptionUseCase createSubscriptionUseCase;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:15:30Z"), ZoneOffset.UTC);

    @InjectMocks
    private CreateInitialSubscriptionService service;

    private static InitialContractTemplate plantilla(int diasDePrueba) {
        return new InitialContractTemplate(3L, 100L, "CORE", "Nucleo", SubscriptionItemType.MODULE,
                null, 2, 1, new BigDecimal("179000.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED, 5, diasDePrueba);
    }

    private static SubscriptionDto contratoCreado() {
        return SubscriptionDto.from(new Subscription(7L, "SUS-2026-00042", EMPRESA, null, 3L,
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1,
                LocalDate.of(2026, 1, 31), null, null, 5, null, true, null, null, 0L, true));
    }

    private static CreateSubscriptionCommand capturarComando(CreateSubscriptionUseCase useCase) {
        ArgumentCaptor<CreateSubscriptionCommand> captor = ArgumentCaptor
                .forClass(CreateSubscriptionCommand.class);
        verify(useCase).execute(captor.capture());
        return captor.getValue();
    }

    private static InitialCapacityTemplate capacidad(Long id, CapacityUnit unidad, int incluidas,
            int minimo) {
        return new InitialCapacityTemplate(id, "CAP_" + unidad.name(), "Capacidad " + unidad.name(),
                unidad, incluidas, minimo, new BigDecimal("12000.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED);
    }

    /** El minimo operable: una sede y un usuario, que es lo que el alta consume. */
    private static List<InitialCapacityTemplate> capacidadesDelMinimo() {
        return List.of(capacidad(200L, CapacityUnit.BRANCH, 0, 1),
                capacidad(201L, CapacityUnit.USER, 0, 1));
    }

    @Nested
    @DisplayName("Con catalogo sembrado")
    class ConCatalogo {

        /**
         * Se llama a mano en cada caso —y no desde un {@code @BeforeEach}— porque dos
         * de ellos siembran capacidades distintas: con la version generica puesta
         * antes, {@code STRICT_STUBS} la denunciaria como stub inservible.
         */
        private void elCatalogoConcedeElMinimoOperable() {
            when(platformCatalogPort.findInitialCapacityTemplates(any()))
                    .thenReturn(capacidadesDelMinimo());
        }

        @Test
        @DisplayName("firma la linea del nucleo y una linea por cada capacidad del minimo")
        void firmaElNucleoYSusCapacidades() {
            elCatalogoConcedeElMinimoOperable();
            // Firmar solo el nucleo es el defecto #490: el contrato quedaba valido y la
            // empresa moria un paso despues, al crear su propia sede principal, con un
            // 404 de capacidad no contratada.
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            CreateSubscriptionCommand comando = capturarComando(createSubscriptionUseCase);
            assertThat(comando.companyId()).isEqualTo(EMPRESA);
            assertThat(comando.priceListId()).isEqualTo(3L);
            assertThat(comando.items()).hasSize(3);
            SubscriptionItemLineCommand linea = comando.items().get(0);
            assertThat(linea.catalogItemId()).isEqualTo(100L);
            assertThat(linea.itemCode()).isEqualTo("CORE");
            assertThat(linea.quantity()).isEqualTo(1);
            assertThat(linea.capacityUnit()).isNull();
            assertThat(comando.items()).extracting(SubscriptionItemLineCommand::capacityUnit)
                    .containsExactly(null, CapacityUnit.BRANCH, CapacityUnit.USER);
            assertThat(comando.items()).extracting(SubscriptionItemLineCommand::itemType)
                    .containsExactly(SubscriptionItemType.MODULE, SubscriptionItemType.CAPACITY,
                            SubscriptionItemType.CAPACITY);
        }

        @Test
        @DisplayName("la capacidad se firma con cantidad al menos uno: un techo cero no abre nada")
        void laCapacidadNaceConTechoUtil() {
            // El techo que acaba en company_capacities es included_quantity + quantity.
            // Con cantidad cero la fila existiria y seguiria sin dejar crear la sede.
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(platformCatalogPort.findInitialCapacityTemplates(any()))
                    .thenReturn(List.of(capacidad(200L, CapacityUnit.BRANCH, 0, 0),
                            capacidad(201L, CapacityUnit.USER, 0, 0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            assertThat(capturarComando(createSubscriptionUseCase).items())
                    .filteredOn(linea -> linea.capacityUnit() != null)
                    .allSatisfy(linea -> assertThat(linea.quantity()).isEqualTo(1));
        }

        @Test
        @DisplayName("congela precio, IVA y lo incluido tambien en las lineas de capacidad")
        void congelaLaTarifaDeLaCapacidad() {
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(platformCatalogPort.findInitialCapacityTemplates(any()))
                    .thenReturn(List.of(capacidad(200L, CapacityUnit.BRANCH, 2, 1),
                            capacidad(201L, CapacityUnit.USER, 0, 1)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            SubscriptionItemLineCommand sede = capturarComando(createSubscriptionUseCase).items()
                    .get(1);
            assertThat(sede.itemCode()).isEqualTo("CAP_BRANCH");
            assertThat(sede.includedQuantity()).isEqualTo(2);
            assertThat(sede.unitAmount()).isEqualByComparingTo("12000.00");
            assertThat(sede.taxRate()).isEqualByComparingTo("19.00");
            assertThat(sede.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
            assertThat(sede.effectiveFrom()).isEqualTo(ENERO_1);
            assertThat(sede.effectiveTo()).isNull();
        }

        @Test
        @DisplayName("congela precio, IVA y lo incluido de la tarifa del dia de la firma")
        void congelaLaTarifa() {
            elCatalogoConcedeElMinimoOperable();
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            SubscriptionItemLineCommand linea = capturarComando(createSubscriptionUseCase).items()
                    .get(0);
            assertThat(linea.unitAmount()).isEqualByComparingTo("179000.00");
            assertThat(linea.taxRate()).isEqualByComparingTo("19.00");
            assertThat(linea.includedQuantity()).isEqualTo(2);
            assertThat(linea.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
        }

        @Test
        @DisplayName("con dias de prueba configurados el contrato nace TRIALING y con fecha de fin")
        void naceTrialing() {
            elCatalogoConcedeElMinimoOperable();
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(15)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            CreateSubscriptionCommand comando = capturarComando(createSubscriptionUseCase);
            assertThat(comando.status()).isEqualTo(SubscriptionStatus.TRIALING);
            assertThat(comando.trialEndDate()).isEqualTo(LocalDate.of(2026, 1, 16));
        }

        @Test
        @DisplayName("sin dias de prueba nace ACTIVE y sin fecha de prueba")
        void naceActive() {
            elCatalogoConcedeElMinimoOperable();
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            CreateSubscriptionCommand comando = capturarComando(createSubscriptionUseCase);
            assertThat(comando.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(comando.trialEndDate()).isNull();
        }

        @Test
        @DisplayName("el periodo mensual cubre hasta la vispera del siguiente ciclo")
        void periodoMensual() {
            elCatalogoConcedeElMinimoOperable();
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            CreateSubscriptionCommand comando = capturarComando(createSubscriptionUseCase);
            assertThat(comando.currentPeriodStart()).isEqualTo(ENERO_1);
            assertThat(comando.currentPeriodEnd()).isEqualTo(LocalDate.of(2026, 1, 31));
            assertThat(comando.nextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        @DisplayName("el ciclo anual pide su propio precio y cubre un ano")
        void periodoAnual() {
            elCatalogoConcedeElMinimoOperable();
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.ANNUAL))
                    .thenReturn(Optional.of(plantilla(0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(
                    new CreateInitialSubscriptionCommand(EMPRESA, BillingCycle.ANNUAL, ENERO_1));

            CreateSubscriptionCommand comando = capturarComando(createSubscriptionUseCase);
            assertThat(comando.billingCycle()).isEqualTo(BillingCycle.ANNUAL);
            assertThat(comando.currentPeriodEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("delega los dias de gracia: no los fija, los deja resolver")
        void heredaLaGracia() {
            elCatalogoConcedeElMinimoOperable();
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            // Va en null a proposito. El valor por defecto lo resuelve
            // CreateSubscriptionService desde platform_billing_config, que es el unico
            // sitio que lo decide; fijarlo aqui tambien es como los dos caminos de alta
            // divergieron y un contrato por API nacia con gracia cero (#467).
            assertThat(capturarComando(createSubscriptionUseCase).graceDays()).isNull();
        }
    }

    @Nested
    @DisplayName("Sin catalogo sembrado")
    class SinCatalogo {

        @Test
        @DisplayName("no crea nada: revertir el alta entera es la conducta correcta")
        void noCreaNada() {
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1)))
                    .isInstanceOf(PlatformCatalogNotConfiguredForSubscriptionException.class)
                    .hasMessageContaining(EMPRESA.toString());

            // Ni contrato vacio ni empresa a medias: la unica salida es que no nazca.
            verify(createSubscriptionUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Con nucleo pero sin las capacidades del minimo (#490)")
    class SinCapacidadesDelMinimo {

        /**
         * Este es exactamente el estado que dejaba la semilla de laboratorio: seis
         * articulos, todos {@code MODULE} o {@code BUNDLE}, ninguno {@code CAPACITY}.
         * El contrato se firmaba, la empresa nacia, y el alta moria tres pasos despues
         * al reservar la sede principal con un {@code 404 COMPANY_CAPACITY_NOT_FOUND}
         * que apuntaba al recalculo de permisos y no al catalogo.
         */
        @Test
        @DisplayName("no firma nada: una empresa que no puede crear su sede no puede nacer")
        void noFirmaSinCapacidades() {
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(platformCatalogPort.findInitialCapacityTemplates(any())).thenReturn(List.of());

            assertThatThrownBy(() -> service
                    .execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1)))
                    .isInstanceOf(PlatformCatalogNotConfiguredForSubscriptionException.class)
                    .hasMessageContaining("BRANCH").hasMessageContaining("USER")
                    .hasMessageContaining(EMPRESA.toString());

            verify(createSubscriptionUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una sola de las dos tampoco basta, y el mensaje nombra la que falta")
        void noFirmaConMediaCapacidad() {
            // El alta consume BRANCH y despues USER. Conceder solo la primera mueve el
            // fallo un paso, no lo arregla.
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(platformCatalogPort.findInitialCapacityTemplates(any()))
                    .thenReturn(List.of(capacidad(200L, CapacityUnit.BRANCH, 0, 1)));

            assertThatThrownBy(() -> service
                    .execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1)))
                    .isInstanceOf(PlatformCatalogNotConfiguredForSubscriptionException.class)
                    .hasMessageContaining("USER");

            verify(createSubscriptionUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("las unidades que el alta no consume no se exigen")
        void noExigeTerminalNiAlmacenamiento() {
            // Exigir TERMINAL le negaria el registro a una plataforma que no venda
            // terminales de caja. Lo que se compra despues se contrata despues.
            when(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .thenReturn(Optional.of(plantilla(0)));
            when(platformCatalogPort.findInitialCapacityTemplates(any()))
                    .thenReturn(capacidadesDelMinimo());
            when(createSubscriptionUseCase.execute(any())).thenReturn(contratoCreado());

            service.execute(new CreateInitialSubscriptionCommand(EMPRESA, null, ENERO_1));

            assertThat(capturarComando(createSubscriptionUseCase).items())
                    .extracting(SubscriptionItemLineCommand::capacityUnit)
                    .doesNotContain(CapacityUnit.TERMINAL, CapacityUnit.STORAGE_GB);
        }
    }
}
