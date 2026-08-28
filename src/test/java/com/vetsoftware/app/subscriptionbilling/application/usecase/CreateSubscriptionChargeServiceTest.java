package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionAmendmentValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.NonBillableSubscriptionItemException;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Devengar un cargo, y las tres referencias que tiene que comprobar antes.
 *
 * <p>
 * <b>Es la mitad de codigo del caso de uso que cerro #418.</b> Antes, el
 * {@code subscriptionItemId} y el {@code amendmentId} no se comprobaban: la FK
 * compuesta los rechazaba en la base, pero como una violacion de constraint
 * convertida en 500 a mitad del cierre mensual, y con el operador teniendo que
 * ir al log de MySQL para saber cual de los cinco ids del cuerpo estaba mal. El
 * escenario real es prosaico y por eso ocurre: dos pestanas abiertas, ids
 * consecutivos, y se copia el otrosi de otra clinica.
 *
 * <p>
 * Lo que estos casos fijan, por tanto, no es solo que se rechace —eso ya lo
 * hacia la base— sino <b>que se rechace nombrando el campo y sin escribir
 * nada</b>. Por eso cada caso de fallo lleva su {@code verifyNoInteractions}
 * sobre el repositorio: un cargo a medias es un devengo que despues no cuadra
 * con ninguna factura.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSubscriptionChargeService — devengar comprobando las tres referencias")
class CreateSubscriptionChargeServiceTest {

    private static final Long EMPRESA = 900L;
    private static final Long OTRA_EMPRESA = 901L;
    private static final Long CONTRATO = 970L;
    private static final Long LINEA = 972L;
    private static final Long OTROSI = 8_100L;
    private static final LocalDate INICIO = LocalDate.of(2026, 2, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 2, 28);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 2, 1, 10, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private SubscriptionChargeRepository repository;
    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private SubscriptionItemValidationPort itemValidationPort;
    @Mock
    private SubscriptionAmendmentValidationPort amendmentValidationPort;
    @Captor
    private ArgumentCaptor<SubscriptionCharge> guardado;

    @Mock
    private SubscriptionBillingMetrics metrics;
    @Mock
    private SubscriptionBillingAuditPort audit;

    private CreateSubscriptionChargeService service;

    @BeforeEach
    void montar() {
        service = new CreateSubscriptionChargeService(repository, subscriptionQueryPort,
                itemValidationPort, amendmentValidationPort, metrics, audit, RELOJ);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda el devengo PENDING con el contrato resuelto y la fecha del reloj"
                + " inyectado")
        void guarda_el_devengo_pendiente_con_el_contrato_resuelto() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.PAID));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionChargeDto dto = service.execute(comando(LINEA, null));

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(cargo -> {
                assertThat(cargo.getCompanyId()).isEqualTo(EMPRESA);
                assertThat(cargo.getSubscriptionId()).isEqualTo(CONTRATO);
                assertThat(cargo.getSubscriptionItemId()).isEqualTo(LINEA);
                assertThat(cargo.getStatus()).isEqualTo(ChargeStatus.PENDING);
                assertThat(cargo.getSubtotalAmount()).isEqualByComparingTo("100000.00");
                assertThat(cargo.getCreatedDate()).isEqualTo(AHORA);
            });
            assertThat(dto.subscriptionId()).isEqualTo(CONTRATO);
            assertThat(dto.status()).isEqualTo(ChargeStatus.PENDING);
        }

        @Test
        @DisplayName("el contrato que se guarda es el que devolvio el puerto, no el que venia"
                + " en el comando")
        void guarda_el_contrato_que_devolvio_el_puerto() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, EMPRESA)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(null, null));

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getSubscriptionId()).isEqualTo(CONTRATO);
        }
    }

    @Nested
    @DisplayName("Referencias opcionales")
    class ReferenciasOpcionales {

        @Test
        @DisplayName("sin linea ni otrosi no se consulta ninguno de los dos puertos: un cargo"
                + " puede no nacer de ninguna linea concreta")
        void sin_linea_ni_otrosi_no_consulta_los_puertos() {
            contratoPropio();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(null, null));

            verifyNoInteractions(itemValidationPort, amendmentValidationPort);
        }

        @Test
        @DisplayName("con linea y otrosi se comprueban los dos, cada uno contra su empresa")
        void con_linea_y_otrosi_se_comprueban_los_dos() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.PAID));
            when(amendmentValidationPort.existsInCompany(OTROSI, EMPRESA)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(LINEA, OTROSI));

            verify(itemValidationPort).findChargeModeInCompany(LINEA, EMPRESA);
            verify(amendmentValidationPort).existsInCompany(OTROSI, EMPRESA);
        }
    }

    @Nested
    @DisplayName("Tenancy — el defecto de #418")
    class Tenancy {

        @Test
        @DisplayName("una linea de otra clinica se rechaza nombrando el campo y no escribe nada")
        void una_linea_ajena_se_rechaza_nombrando_el_campo() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(LINEA, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription item not found for company")
                    .hasMessageContaining(String.valueOf(LINEA));

            verifyNoInteractions(repository, amendmentValidationPort);
        }

        @Test
        @DisplayName("un otrosi de otra clinica se rechaza nombrando el campo y no escribe nada:"
                + " es el error de dos pestanas con ids consecutivos")
        void un_otrosi_ajeno_se_rechaza_nombrando_el_campo() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.PAID));
            when(amendmentValidationPort.existsInCompany(OTROSI, EMPRESA)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando(LINEA, OTROSI)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription amendment not found for company")
                    .hasMessageContaining(String.valueOf(OTROSI));

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un contrato que no es de la empresa no existe para este caso de uso, y no"
                + " se llega a comprobar ninguna otra referencia")
        void un_contrato_ajeno_no_existe_para_este_caso_de_uso() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(LINEA, OTROSI)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found: " + CONTRATO);

            verifyNoInteractions(repository, itemValidationPort, amendmentValidationPort);
        }

        @Test
        @DisplayName("si el puerto devolviera un contrato de otra empresa, la comprobacion"
                + " redundante del VO lo ataja: es la tercera capa, y aqui se cobra")
        void un_contrato_con_empresa_distinta_lo_ataja_el_vo() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, OTRA_EMPRESA)));

            assertThatThrownBy(() -> service.execute(comando(null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to company " + EMPRESA);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Prorrateo")
    class Prorrateo {

        @Test
        @DisplayName("los dos numeros del prorrateo llegan al cargo: es lo que lo hace"
                + " reconstruible")
        void los_dos_numeros_del_prorrateo_llegan_al_cargo() {
            contratoPropio();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new CreateSubscriptionChargeCommand(EMPRESA, CONTRATO, null,
                    ChargeType.PRORATION, "Ampliacion a mitad de mes", INICIO, FIN, BigDecimal.ONE,
                    new BigDecimal("100000.00"), new BigDecimal("50000.00"),
                    new BigDecimal("19.00"), TaxTreatment.TAXED, 14, 28, null));

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getProration()).satisfies(proration -> {
                assertThat(proration.prorationDays()).isEqualTo(14);
                assertThat(proration.periodDays()).isEqualTo(28);
            });
        }

        @Test
        @DisplayName("sin prorrateo el cargo no lleva ninguna base, y no escribe un cero que"
                + " pareceria un prorrateo de cero dias")
        void sin_prorrateo_el_cargo_no_lleva_base() {
            contratoPropio();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(null, null));

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getProration()).isNull();
        }
    }

    @Nested
    @DisplayName("R-TRIAL-14 · solo la linea que cobra devenga")
    class SoloLaLineaQueCobraDevenga {

        /**
         * <b>El caso violador, tal como lo escribe el catalogo de reglas.</b>
         *
         * <p>
         * El peligro no es que se cobre de mas por descuido: es que <strong>el precio
         * real sigue guardado dentro de la linea gratuita</strong>. Una linea en prueba
         * lleva su cuota completa en {@code unit_amount} porque es la que se cobrara el
         * dia que convierta; lo unico que la separa de un cargo es el modo. Una
         * consulta de cobro que filtre solo por vigencia y olvide el modo no emite un
         * importe raro que alguien note: emite la cuota correcta, a todos los clientes
         * en prueba, el mismo mes.
         */
        @Test
        @DisplayName("una linea en prueba no devenga: el precio vive dentro de la linea"
                + " gratuita y cobrarlo factura la cuota entera a quien esta probando")
        void una_consulta_de_facturacion_que_omite_charge_mode_le_cobra_a_los_clientes_en_prueba() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.TRIAL));

            assertThatThrownBy(() -> service.execute(comando(LINEA, null)))
                    .isInstanceOf(NonBillableSubscriptionItemException.class)
                    .hasMessageContaining("TRIAL");

            verify(repository, never()).save(any());
        }

        /** El gratis con techo tampoco: no caduca, asi que cobrarlo no prescribe. */
        @Test
        @DisplayName("una linea FREE_LIMITED tampoco devenga")
        void una_linea_free_limited_tampoco_devenga() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.FREE_LIMITED));

            assertThatThrownBy(() -> service.execute(comando(LINEA, null)))
                    .isInstanceOf(NonBillableSubscriptionItemException.class);

            verify(repository, never()).save(any());
        }

        /** Y la prueba vencida en solo lectura: se le dejo ver, no se le cobra. */
        @Test
        @DisplayName("una linea EXPIRED_READ_ONLY tampoco devenga")
        void una_linea_expired_read_only_tampoco_devenga() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.EXPIRED_READ_ONLY));

            assertThatThrownBy(() -> service.execute(comando(LINEA, null)))
                    .isInstanceOf(NonBillableSubscriptionItemException.class);

            verify(repository, never()).save(any());
        }

        /**
         * R-TRIAL-13: el estado del contrato no decide nada. Un contrato TRIALING con
         * una linea de pago obligatorio SI devenga, y ese es el caso que la trampa
         * anterior --descartar los contratos en prueba-- dejaba sin facturar.
         */
        @Test
        @DisplayName("una linea PAID devenga aunque su contrato este en prueba")
        void una_linea_paid_devenga_aunque_el_contrato_este_en_prueba() {
            contratoPropio();
            when(itemValidationPort.findChargeModeInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(ItemChargeMode.PAID));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(LINEA, null));

            verify(repository).save(any());
        }
    }

    private void contratoPropio() {
        when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, EMPRESA)));
    }

    private static CreateSubscriptionChargeCommand comando(Long lineaId, Long otrosiId) {
        return new CreateSubscriptionChargeCommand(EMPRESA, CONTRATO, lineaId, ChargeType.RECURRING,
                "Cuota febrero", INICIO, FIN, BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                null, otrosiId);
    }
}
