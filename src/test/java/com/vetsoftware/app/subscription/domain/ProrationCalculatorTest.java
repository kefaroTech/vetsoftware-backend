package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La aritmetica del prorrateo, y sobre todo <b>el caso en que se niega a
 * responder</b>.
 */
@DisplayName("ProrationCalculator — la fraccion del periodo, y el cero que es un error")
class ProrationCalculatorTest {

    /** Marzo completo: 31 dias. */
    private static final BillingPeriod MARZO = new BillingPeriod(LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31));

    @Nested
    @DisplayName("La formula")
    class Formula {

        @Test
        @DisplayName("un alta a mitad de ciclo cobra la fraccion de dias que queda")
        void alta_a_mitad_de_ciclo() {
            // Del 17 al 31 son 15 dias de 31.
            Proration proration = ProrationCalculator.onCurrentPeriod(new BigDecimal("310000.00"),
                    MARZO, new EffectivePeriod(LocalDate.of(2026, 3, 17), null));

            assertThat(proration.prorationDays()).isEqualTo(15);
            assertThat(proration.periodDays()).isEqualTo(31);
            assertThat(proration.amount()).isEqualByComparingTo("150000.00");
        }

        @Test
        @DisplayName("una baja produce el mismo importe con signo contrario")
        void baja_produce_abono() {
            Proration proration = ProrationCalculator.onCurrentPeriod(new BigDecimal("-310000.00"),
                    MARZO, new EffectivePeriod(LocalDate.of(2026, 3, 17), null));

            assertThat(proration.amount()).isEqualByComparingTo("-150000.00");
        }

        @Test
        @DisplayName("un cambio que cubre el periodo entero cobra el ciclo completo")
        void periodo_entero() {
            Proration proration = ProrationCalculator.onCurrentPeriod(new BigDecimal("310000.00"),
                    MARZO, new EffectivePeriod(LocalDate.of(2026, 3, 1), null));

            assertThat(proration.prorationDays()).isEqualTo(31);
            assertThat(proration.amount()).isEqualByComparingTo("310000.00");
        }
    }

    @Nested
    @DisplayName("Cero dias es un error, nunca un resultado")
    class CeroDias {

        /**
         * <b>La regla que esta prueba defiende.</b> Antes, un tramo que no tocaba el
         * periodo en curso devolvia {@code 0.00} y el otrosi se guardaba con importe
         * cero: firmado, inmutable y con toda la pinta de estar bien. El caso real es
         * el del periodo que nunca avanzaba —{@code renewPeriod} no tenia llamador— asi
         * que meses despues la conversion de una linea de prueba a pago se medi­a
         * contra un periodo ya cerrado y el cliente estrenaba su plan pagando cero.
         */
        @Test
        @DisplayName("un tramo que empieza despues del periodo revienta en vez de dar cero")
        void tramo_posterior_al_periodo_revienta() {
            assertThatThrownBy(
                    () -> ProrationCalculator.onCurrentPeriod(new BigDecimal("310000.00"), MARZO,
                            new EffectivePeriod(LocalDate.of(2026, 5, 1), null)))
                    .isInstanceOf(ZeroDayProrationException.class)
                    .hasMessageContaining("un prorrateo de cero dias es un error");
        }

        @Test
        @DisplayName("un tramo que termino antes del periodo tambien revienta")
        void tramo_anterior_al_periodo_revienta() {
            assertThatThrownBy(
                    () -> ProrationCalculator.onCurrentPeriod(new BigDecimal("310000.00"), MARZO,
                            new EffectivePeriod(LocalDate.of(2026, 1, 1),
                                    LocalDate.of(2026, 2, 1))))
                    .isInstanceOf(ZeroDayProrationException.class);
        }

        @Test
        @DisplayName("el error se traduce a 400 porque es un IllegalArgumentException")
        void es_un_illegal_argument() {
            // Deliberado: el GlobalExceptionHandler ya traduce IllegalArgumentException a
            // 400, que es la respuesta correcta -la fecha efectiva pedida cae fuera del
            // periodo- y asi no hay que tocar un fichero compartido para conseguirla.
            assertThat(new ZeroDayProrationException(MARZO.start(), MARZO.end()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("el mensaje nombra el periodo contra el que se midio")
        void el_mensaje_nombra_el_periodo() {
            assertThatThrownBy(() -> ProrationCalculator.onCurrentPeriod(new BigDecimal("1.00"),
                    MARZO, new EffectivePeriod(LocalDate.of(2026, 5, 1), null)))
                    .hasMessageContaining("2026-03-01").hasMessageContaining("2026-03-31");
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("exige el delta del ciclo")
        void exige_delta() {
            assertThatThrownBy(() -> ProrationCalculator.onCurrentPeriod(null, MARZO,
                    new EffectivePeriod(LocalDate.of(2026, 3, 1), null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cycleDelta is required");
        }

        @Test
        @DisplayName("exige el periodo de facturacion")
        void exige_periodo() {
            assertThatThrownBy(() -> ProrationCalculator.onCurrentPeriod(BigDecimal.ONE, null,
                    new EffectivePeriod(LocalDate.of(2026, 3, 1), null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billing period is required");
        }
    }
}
