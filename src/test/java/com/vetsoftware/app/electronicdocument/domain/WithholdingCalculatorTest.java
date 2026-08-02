package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Retenciones que practica el adquiriente agente retenedor. Tres reglas que no pueden desviarse:
 * reteFuente/reteICA van sobre la base gravable, reteIVA solo sobre el IVA generado, y la cuantía
 * mínima (4 UVT) apaga reteFuente/reteIVA pero NO reteICA (los mínimos de ICA son municipales).
 */
class WithholdingCalculatorTest {

    /** UVT 2026 usado como referencia en los escenarios; el cálculo no depende del año, solo del valor. */
    private static final BigDecimal UVT = new BigDecimal("49799");
    private static final BigDecimal RETE_FUENTE_4 = new BigDecimal("4");
    private static final BigDecimal RETE_IVA_15 = new BigDecimal("15");
    private static final BigDecimal RETE_ICA_9_66 = new BigDecimal("9.66");

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Nested
    class AdquirienteNoRetenedor {

        @Test
        void no_practica_ninguna_retencion() {
            WithholdingAmounts result = WithholdingCalculator.compute(
                    false, bd("10000000"), bd("1900000"), RETE_FUENTE_4, RETE_IVA_15, RETE_ICA_9_66, UVT);

            assertThat(result).isSameAs(WithholdingAmounts.NONE);
            assertThat(result.total()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class SobreLaCuantiaMinima {

        @Test
        void aplica_las_tres_retenciones_sobre_sus_bases_correctas() {
            BigDecimal base = bd("1000000");
            BigDecimal iva = bd("190000");

            WithholdingAmounts result = WithholdingCalculator.compute(
                    true, base, iva, RETE_FUENTE_4, RETE_IVA_15, RETE_ICA_9_66, UVT);

            assertThat(result.reteFuente()).isEqualByComparingTo("40000.00");   // 4% de la base
            assertThat(result.reteIva()).isEqualByComparingTo("28500.00");      // 15% del IVA, no de la base
            assertThat(result.reteIca()).isEqualByComparingTo("9660.00");       // 9,66 ‰ de la base
            assertThat(result.total()).isEqualByComparingTo("78160.00");
        }

        @Test
        void la_reteiva_nunca_se_calcula_sobre_la_base() {
            BigDecimal base = bd("1000000");
            BigDecimal iva = bd("190000");

            WithholdingAmounts result = WithholdingCalculator.compute(
                    true, base, iva, BigDecimal.ZERO, RETE_IVA_15, BigDecimal.ZERO, UVT);

            assertThat(result.reteIva()).isEqualByComparingTo("28500.00");
            assertThat(result.reteIva()).isNotEqualByComparingTo("150000.00");
        }

        @Test
        void justo_en_el_umbral_de_4_uvt_si_retiene() {
            BigDecimal base = UVT.multiply(bd("4"));   // exactamente 4 UVT

            WithholdingAmounts result = WithholdingCalculator.compute(
                    true, base, bd("1000"), RETE_FUENTE_4, RETE_IVA_15, BigDecimal.ZERO, UVT);

            assertThat(result.reteFuente()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.reteIva()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    class BajoLaCuantiaMinima {

        @Test
        void no_practica_retefuente_ni_reteiva_pero_si_reteica() {
            BigDecimal base = UVT.multiply(bd("4")).subtract(BigDecimal.ONE);   // un peso por debajo

            WithholdingAmounts result = WithholdingCalculator.compute(
                    true, base, bd("50000"), RETE_FUENTE_4, RETE_IVA_15, RETE_ICA_9_66, UVT);

            assertThat(result.reteFuente()).isEqualByComparingTo("0");
            assertThat(result.reteIva()).isEqualByComparingTo("0");
            assertThat(result.reteIca()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        void una_venta_pequena_de_mostrador_no_genera_retenciones_de_renta() {
            WithholdingAmounts result = WithholdingCalculator.compute(
                    true, bd("50000"), bd("9500"), RETE_FUENTE_4, RETE_IVA_15, BigDecimal.ZERO, UVT);

            assertThat(result.total()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class UvtNoDisponible {

        @Test
        void sin_uvt_no_se_bloquea_la_retencion() {
            // Política documentada: si no hay UVT configurado no se puede evaluar el mínimo → no se bloquea.
            WithholdingAmounts sinUvt = WithholdingCalculator.compute(
                    true, bd("50000"), bd("9500"), RETE_FUENTE_4, RETE_IVA_15, BigDecimal.ZERO, null);

            assertThat(sinUvt.reteFuente()).isEqualByComparingTo("2000.00");
            assertThat(sinUvt.reteIva()).isEqualByComparingTo("1425.00");
        }

        @Test
        void uvt_cero_o_negativo_se_trata_igual_que_ausente() {
            assertThat(WithholdingCalculator.compute(true, bd("50000"), bd("9500"),
                    RETE_FUENTE_4, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).reteFuente())
                    .isEqualByComparingTo("2000.00");
            assertThat(WithholdingCalculator.compute(true, bd("50000"), bd("9500"),
                    RETE_FUENTE_4, BigDecimal.ZERO, BigDecimal.ZERO, bd("-1")).reteFuente())
                    .isEqualByComparingTo("2000.00");
        }
    }

    @Nested
    class TarifasEnCero {

        @Test
        void tarifas_nulas_o_cero_producen_cero_sin_fallar() {
            WithholdingAmounts result = WithholdingCalculator.compute(
                    true, bd("10000000"), bd("1900000"), null, BigDecimal.ZERO, null, UVT);

            assertThat(result.total()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class Agregacion {

        @Test
        void total_suma_las_tres_retenciones() {
            WithholdingAmounts amounts = new WithholdingAmounts(bd("100.50"), bd("20.25"), bd("5.25"));
            assertThat(amounts.total()).isEqualByComparingTo("126.00");
        }

        @Test
        void los_nulos_del_record_se_normalizan_a_cero() {
            WithholdingAmounts amounts = new WithholdingAmounts(null, null, null);

            assertThat(amounts.reteFuente()).isEqualByComparingTo("0");
            assertThat(amounts.reteIva()).isEqualByComparingTo("0");
            assertThat(amounts.reteIca()).isEqualByComparingTo("0");
            assertThat(amounts.total()).isEqualByComparingTo("0");
        }
    }
}
