package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionAmendmentValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mes contable de un cargo lo decide la zona del reloj, no el instante
 * (D-81).
 *
 * <p>
 * {@code SubscriptionCharge.create} deriva del reloj <b>un solo campo</b>:
 * {@code createdDate}, un {@code LocalDateTime.now(clock)}
 * ({@code SubscriptionCharge.java:123}). Todo lo demas —incluido el periodo de
 * servicio— viene entero del comando. Ese unico campo es el que atribuye el
 * devengo a un mes: es la marca por la que el cierre mensual recoge el cargo.
 *
 * <p>
 * <b>El defecto</b>: {@code ClockConfig} devolvia
 * {@code Clock.systemDefaultZone()} y la imagen no declara zona, asi que en
 * produccion el reloj corria en UTC. Un cargo devengado a las 23:59 del 31 de
 * marzo en Bogota sellaba {@code 2026-04-01T04:59} y quedaba atribuido a abril:
 * salia en la factura del periodo equivocado, y el cierre de marzo cuadraba de
 * menos sin que nada fallara.
 *
 * <p>
 * Por eso los dos casos de aqui comparten <b>un mismo instante</b> y solo se
 * diferencian en la zona del {@code Clock.fixed(...)}: si la asercion cambia,
 * lo que cambio fue la zona, que es exactamente lo que hay que sujetar. El caso
 * de UTC no describe lo que el sistema debe hacer —describe la regresion— y
 * esta escrito para volverse rojo por la razon correcta el dia que alguien
 * devuelva el bean a {@code systemDefaultZone()}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSubscriptionChargeService — la zona del reloj decide el mes contable del"
        + " cargo, via createdDate (D-81)")
class CreateSubscriptionChargeServiceZoneTest {

    private static final Long EMPRESA = 900L;
    private static final Long CONTRATO = 970L;
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    /**
     * Las 23:59 del 31 de marzo de 2026 en la pared de Bogota. Un unico instante,
     * el mismo para los dos casos: lo unico que se mueve entre ellos es la zona.
     */
    private static final Instant ULTIMO_MINUTO_DE_MARZO = ZonedDateTime
            .of(LocalDate.of(2026, 3, 31), LocalTime.of(23, 59), ClockConfig.BUSINESS_ZONE)
            .toInstant();

    @Mock
    private SubscriptionChargeRepository repository;
    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private SubscriptionItemValidationPort itemValidationPort;
    @Mock
    private SubscriptionAmendmentValidationPort amendmentValidationPort;

    @Nested
    @DisplayName("Zona de negocio — el reloj arreglado")
    class ZonaDeNegocio {

        @Test
        @DisplayName("un cargo devengado a las 23:59 del 31 de marzo sella createdDate en marzo,"
                + " el dia 31: cae en el mes que lo devengo")
        void un_cargo_de_las_2359_del_31_de_marzo_se_sella_en_marzo() {
            contratoPropio();
            devuelveLoGuardado();

            SubscriptionChargeDto dto = servicioCon(
                    Clock.fixed(ULTIMO_MINUTO_DE_MARZO, ClockConfig.BUSINESS_ZONE))
                    .execute(comando());

            assertThat(dto.createdDate().getMonth()).isEqualTo(Month.MARCH);
            assertThat(dto.createdDate().getDayOfMonth()).isEqualTo(31);
            assertThat(dto.createdDate().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(dto.createdDate()).isEqualTo(LocalDateTime.of(2026, 3, 31, 23, 59));
        }
    }

    @Nested
    @DisplayName("Regresion D-81 — el mismo instante leido en UTC")
    class RegresionUtc {

        @Test
        @DisplayName("con el reloj en UTC el mismo instante sella createdDate en abril, dia 1: el"
                + " cargo se atribuye al mes siguiente y se factura en el periodo equivocado")
        void el_mismo_instante_en_utc_se_sella_en_abril() {
            contratoPropio();
            devuelveLoGuardado();

            SubscriptionChargeDto dto = servicioCon(
                    Clock.fixed(ULTIMO_MINUTO_DE_MARZO, ZoneOffset.UTC)).execute(comando());

            assertThat(dto.createdDate().getMonth()).isEqualTo(Month.APRIL);
            assertThat(dto.createdDate().getDayOfMonth()).isEqualTo(1);
            assertThat(dto.createdDate().toLocalDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(dto.createdDate()).isEqualTo(LocalDateTime.of(2026, 4, 1, 4, 59));
        }

        @Test
        @DisplayName("la zona equivocada no mueve el periodo de servicio, que viene entero del"
                + " comando: lo unico que se desplaza —y basta— es la marca contable")
        void la_zona_no_mueve_el_periodo_de_servicio_solo_la_marca() {
            contratoPropio();
            devuelveLoGuardado();

            SubscriptionChargeDto dto = servicioCon(
                    Clock.fixed(ULTIMO_MINUTO_DE_MARZO, ZoneOffset.UTC)).execute(comando());

            assertThat(dto.servicePeriodStart()).isEqualTo(INICIO);
            assertThat(dto.servicePeriodEnd()).isEqualTo(FIN);
            assertThat(dto.createdDate().toLocalDate()).isAfter(dto.servicePeriodEnd());
        }
    }

    @Mock
    private SubscriptionBillingMetrics metrics;
    @Mock
    private SubscriptionBillingAuditPort audit;

    private CreateSubscriptionChargeService servicioCon(Clock reloj) {
        return new CreateSubscriptionChargeService(repository, subscriptionQueryPort,
                itemValidationPort, amendmentValidationPort, metrics, audit, reloj);
    }

    private void contratoPropio() {
        when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, EMPRESA)));
    }

    private void devuelveLoGuardado() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Cuota de marzo, sin linea ni otrosi: aqui lo que se prueba es la marca. */
    private static CreateSubscriptionChargeCommand comando() {
        return new CreateSubscriptionChargeCommand(EMPRESA, CONTRATO, null, ChargeType.RECURRING,
                "Cuota marzo", INICIO, FIN, BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                null, null);
    }
}
