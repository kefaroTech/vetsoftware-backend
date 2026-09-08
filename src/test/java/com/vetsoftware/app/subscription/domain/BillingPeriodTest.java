package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BillingPeriod - el periodo de facturacion en curso")
class BillingPeriodTest {

    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate ENERO_31 = LocalDate.of(2026, 1, 31);
    private static final BillingPeriod ENERO = new BillingPeriod(ENERO_1, ENERO_31);

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("el inicio es obligatorio")
        void el_inicio_es_obligatorio() {
            assertThatThrownBy(() -> new BillingPeriod(null, ENERO_31))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("start");
        }

        @Test
        @DisplayName("el fin es obligatorio")
        void el_fin_es_obligatorio() {
            assertThatThrownBy(() -> new BillingPeriod(ENERO_1, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("end");
        }

        @Test
        @DisplayName("el fin no puede ser anterior al inicio")
        void el_fin_no_puede_ser_anterior_al_inicio() {
            assertThatThrownBy(() -> new BillingPeriod(ENERO_31, ENERO_1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be before start");
        }

        @Test
        @DisplayName("of() toma el periodo vigente del contrato")
        void of_toma_el_periodo_vigente_del_contrato() {
            Subscription contrato = Subscription.create("SUS-2026-00001", 1L, null, 1L,
                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1,
                    ENERO_31, null, null, 0, true);

            BillingPeriod periodo = BillingPeriod.of(contrato);

            assertThat(periodo.start()).isEqualTo(ENERO_1);
            assertThat(periodo.end()).isEqualTo(ENERO_31);
        }

        @Test
        @DisplayName("of() exige un contrato")
        void of_exige_un_contrato() {
            assertThatThrownBy(() -> BillingPeriod.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subscription is required");
        }
    }

    @Nested
    @DisplayName("days()")
    class Dias {

        @Test
        @DisplayName("cuenta los dos extremos: enero completo son 31 dias, no 30")
        void cuenta_los_dos_extremos() {
            assertThat(ENERO.days()).isEqualTo(31);
        }

        @Test
        @DisplayName("un periodo de un solo dia cuenta uno")
        void un_periodo_de_un_solo_dia_cuenta_uno() {
            assertThat(new BillingPeriod(ENERO_1, ENERO_1).days()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("coveredRange() — acota el tramo por los dos extremos del periodo")
    class CoveredRangeTest {

        @Test
        @DisplayName("un tramo que ya estaba abierto antes del periodo se acota al primer dia del "
                + "periodo, nunca a su propio inicio")
        void tramo_retroactivo_se_acota_al_inicio_del_periodo() {
            EffectivePeriod tramo = EffectivePeriod.openFrom(LocalDate.of(2025, 12, 1));

            BillingPeriod.CoveredRange range = ENERO.coveredRange(tramo);

            assertThat(range.start()).isEqualTo(ENERO_1);
            assertThat(range.end()).isEqualTo(ENERO_31);
        }

        @Test
        @DisplayName("un tramo abierto que empieza dentro del periodo conserva su propio inicio y "
                + "se acota al fin del periodo")
        void tramo_abierto_dentro_del_periodo_se_acota_al_fin() {
            EffectivePeriod tramo = EffectivePeriod.openFrom(LocalDate.of(2026, 1, 17));

            BillingPeriod.CoveredRange range = ENERO.coveredRange(tramo);

            assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 17));
            assertThat(range.end()).isEqualTo(ENERO_31);
        }

        @Test
        @DisplayName("un tramo que termina dentro del periodo se acota al dia anterior a su propio "
                + "fin: el intervalo semiabierto no cubre el dia de effectiveTo")
        void tramo_que_termina_dentro_del_periodo_se_acota_al_dia_anterior() {
            EffectivePeriod tramo = new EffectivePeriod(LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 11));

            BillingPeriod.CoveredRange range = ENERO.coveredRange(tramo);

            assertThat(range.start()).isEqualTo(ENERO_1);
            assertThat(range.end()).isEqualTo(LocalDate.of(2026, 1, 10));
        }

        @Test
        @DisplayName("un tramo que no toca este periodo devuelve un rango invertido, y el numerador "
                + "de daysCoveredBy sobre el mismo tramo da cero")
        void tramo_fuera_del_periodo_da_rango_invertido() {
            EffectivePeriod tramoFuturo = EffectivePeriod.openFrom(LocalDate.of(2026, 3, 1));

            BillingPeriod.CoveredRange range = ENERO.coveredRange(tramoFuturo);

            assertThat(range.end()).isBefore(range.start());
            assertThat(ENERO.daysCoveredBy(tramoFuturo)).isZero();
        }

        @Test
        @DisplayName("exige el tramo afectado")
        void exige_el_tramo_afectado() {
            assertThatThrownBy(() -> ENERO.coveredRange(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("affected period is required");
        }
    }

    @Nested
    @DisplayName("Coherencia entre coveredRange() y daysCoveredBy()")
    class Coherencia {

        @Test
        @DisplayName("con cobertura positiva, los dias que cuenta daysCoveredBy son exactamente los "
                + "que separan los dos extremos de coveredRange, ambos inclusive")
        void daysCoveredBy_es_la_distancia_entre_los_extremos_de_coveredRange() {
            EffectivePeriod tramo = EffectivePeriod.openFrom(LocalDate.of(2026, 1, 17));

            BillingPeriod.CoveredRange range = ENERO.coveredRange(tramo);
            int distancia = (int) (range.end().toEpochDay() - range.start().toEpochDay()) + 1;

            assertThat(ENERO.daysCoveredBy(tramo)).isEqualTo(distancia);
            assertThat(ENERO.daysCoveredBy(tramo)).isEqualTo(15);
        }

        @Test
        @DisplayName("un tramo que cubre el periodo entero cuenta lo mismo que days()")
        void tramo_que_cubre_todo_el_periodo_cuenta_como_days() {
            EffectivePeriod tramoCompleto = EffectivePeriod.openFrom(ENERO_1);

            assertThat(ENERO.daysCoveredBy(tramoCompleto)).isEqualTo(ENERO.days());
        }
    }
}
