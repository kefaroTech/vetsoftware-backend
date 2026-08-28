package com.vetsoftware.app.revenuerecognitionline.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.revenuerecognitionline.testsupport.RevenueRecognitionLineMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("RevenueRecognitionLine — invariantes del libro que solo se agrega")
class RevenueRecognitionLineTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 5, 8, 0);

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir ocho
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = 1L;
        private Long companyId = 9L;
        private Long chargeId = 42L;
        private String periodKey = "2026-03";
        private String postingPeriod = "2026-03";
        private BigDecimal recognizedAmount = new BigDecimal("100.00");
        private RecognitionMethod method = RecognitionMethod.STRAIGHT_LINE_DAYS;
        private LocalDateTime createdDate = CREADO;

        private Builder companyId(Long v) {
            this.companyId = v;
            return this;
        }

        private Builder chargeId(Long v) {
            this.chargeId = v;
            return this;
        }

        private Builder periodKey(String v) {
            this.periodKey = v;
            return this;
        }

        private Builder postingPeriod(String v) {
            this.postingPeriod = v;
            return this;
        }

        private Builder recognizedAmount(BigDecimal v) {
            this.recognizedAmount = v;
            return this;
        }

        private Builder method(RecognitionMethod v) {
            this.method = v;
            return this;
        }

        private Builder createdDate(LocalDateTime v) {
            this.createdDate = v;
            return this;
        }

        private RevenueRecognitionLine build() {
            return new RevenueRecognitionLine(id, companyId, chargeId, periodKey, postingPeriod,
                    recognizedAmount, method, createdDate);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            RevenueRecognitionLine line = valido().build();

            assertThat(line.getId()).isEqualTo(1L);
            assertThat(line.getCompanyId()).isEqualTo(9L);
            assertThat(line.getChargeId()).isEqualTo(42L);
            assertThat(line.getPeriodKey()).isEqualTo("2026-03");
            assertThat(line.getPostingPeriod()).isEqualTo("2026-03");
            assertThat(line.getRecognizedAmount()).isEqualByComparingTo("100.00");
            assertThat(line.getMethod()).isEqualTo(RecognitionMethod.STRAIGHT_LINE_DAYS);
            assertThat(line.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("record() nace sin id: es siempre un insert nuevo en el libro")
        void record_nace_sin_id() {
            RevenueRecognitionLine line = RevenueRecognitionLine.record(9L, 42L, "2026-03",
                    "2026-03", new BigDecimal("100.00"), RecognitionMethod.STRAIGHT_LINE_DAYS,
                    CREADO);

            assertThat(line.getId()).isNull();
            assertThat(line.getCompanyId()).isEqualTo(9L);
            assertThat(line.getChargeId()).isEqualTo(42L);
        }

        @ParameterizedTest
        @EnumSource(RecognitionMethod.class)
        @DisplayName("acepta cualquier metodo del dominio cerrado, incluido el que aun no se usa")
        void acepta_cualquier_metodo_de_reconocimiento(RecognitionMethod method) {
            assertThatCode(() -> valido().method(method).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("chk_rrl_not_backwards — el periodo contable nunca es anterior al imputado")
    class NoHaciaAtras {

        @Test
        @DisplayName("postingPeriod igual a periodKey es valido: se registra en el mismo mes")
        void posting_period_igual_a_period_key_es_valido() {
            assertThatCode(() -> valido().periodKey("2026-03").postingPeriod("2026-03").build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("postingPeriod posterior a periodKey es valido: el hecho tardio")
        void posting_period_posterior_a_period_key_es_valido() {
            assertThatCode(() -> valido().periodKey("2026-01").postingPeriod("2026-03").build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("postingPeriod anterior a periodKey se rechaza: la comparacion de cadenas AAAA-MM "
                + "es la cronologica")
        void posting_period_anterior_a_period_key_se_rechaza() {
            assertThatThrownBy(() -> valido().periodKey("2026-03").postingPeriod("2026-02").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("revenue is never posted backwards");
        }

        @Test
        @DisplayName("el cruce de anio no rompe la comparacion lexicografica: 2027-01 no es "
                + "anterior a 2026-12")
        void el_cruce_de_anio_no_rompe_la_comparacion() {
            assertThatCode(() -> valido().periodKey("2026-12").postingPeriod("2027-01").build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("companyId null",
                            (ThrowingCallable) () -> valido().companyId(null).build(),
                            "companyId is required"),
                    arguments("chargeId null",
                            (ThrowingCallable) () -> valido().chargeId(null).build(),
                            "chargeId is required"),
                    arguments("periodKey null",
                            (ThrowingCallable) () -> valido().periodKey(null).build(),
                            "periodKey is required"),
                    arguments("periodKey en blanco",
                            (ThrowingCallable) () -> valido().periodKey("   ").build(),
                            "periodKey is required"),
                    arguments("periodKey con mes invalido",
                            (ThrowingCallable) () -> valido().periodKey("2026-13").build(),
                            "periodKey must have the form yyyy-MM"),
                    arguments("periodKey con formato corto",
                            (ThrowingCallable) () -> valido().periodKey("2026-3").build(),
                            "periodKey must have the form yyyy-MM"),
                    arguments("postingPeriod null",
                            (ThrowingCallable) () -> valido().postingPeriod(null).build(),
                            "postingPeriod is required"),
                    arguments("postingPeriod con formato invalido",
                            (ThrowingCallable) () -> valido().postingPeriod("2026-00").build(),
                            "postingPeriod must have the form yyyy-MM"),
                    arguments("recognizedAmount null",
                            (ThrowingCallable) () -> valido().recognizedAmount(null).build(),
                            "recognizedAmount is required"),
                    arguments("recognizedAmount cero: no compensa nada y solo ensucia el libro",
                            (ThrowingCallable) () -> valido().recognizedAmount(BigDecimal.ZERO)
                                    .build(),
                            "recognizedAmount must not be zero"),
                    arguments("recognizedAmount con tres decimales",
                            (ThrowingCallable) () -> valido()
                                    .recognizedAmount(new BigDecimal("100.001")).build(),
                            "recognizedAmount must have 2 decimals or fewer"),
                    arguments("method null", (ThrowingCallable) () -> valido().method(null).build(),
                            "method is required"),
                    arguments("createdDate null",
                            (ThrowingCallable) () -> valido().createdDate(null).build(),
                            "createdDate is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("recognizedAmount negativo es valido: es la fila que compensa")
        void recognized_amount_negativo_es_valido() {
            assertThatCode(() -> valido().recognizedAmount(new BigDecimal("-100.00")).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("offsetIn — la fila que compensa")
    class Compensacion {

        @Test
        @DisplayName("compensa con el importe opuesto en un periodo contable distinto y posterior")
        void compensa_con_el_importe_opuesto_en_otro_periodo() {
            RevenueRecognitionLine original = RevenueRecognitionLineMother.renglon();
            LocalDateTime creadoEn = LocalDateTime.of(2026, 4, 2, 9, 0);

            RevenueRecognitionLine compensacion = original.offsetIn("2026-04", creadoEn);

            assertThat(compensacion.getId()).isNull();
            assertThat(compensacion.getCompanyId()).isEqualTo(original.getCompanyId());
            assertThat(compensacion.getChargeId()).isEqualTo(original.getChargeId());
            assertThat(compensacion.getPeriodKey()).isEqualTo(original.getPeriodKey());
            assertThat(compensacion.getPostingPeriod()).isEqualTo("2026-04");
            assertThat(compensacion.getPostingPeriod()).isNotEqualTo(original.getPostingPeriod());
            assertThat(compensacion.getRecognizedAmount())
                    .isEqualByComparingTo(original.getRecognizedAmount().negate());
            assertThat(compensacion.getMethod()).isEqualTo(original.getMethod());
            assertThat(compensacion.getCreatedDate()).isEqualTo(creadoEn);
        }

        @Test
        @DisplayName("isOffset distingue el renglon original del que compensa por el signo")
        void is_offset_distingue_por_el_signo() {
            RevenueRecognitionLine original = RevenueRecognitionLineMother.renglon();

            RevenueRecognitionLine compensacion = original.offsetIn("2026-04",
                    LocalDateTime.of(2026, 4, 2, 9, 0));

            assertThat(original.isOffset()).isFalse();
            assertThat(compensacion.isOffset()).isTrue();
        }
    }
}
