package com.vetsoftware.app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("PhysicalExam — invariantes del examen fisico / constantes vitales")
class PhysicalExamTest {

    private static PhysicalExam completo() {
        return new PhysicalExam(new BigDecimal("38.5"), 90, 22, "Rosadas", "< 2 seg", "Normal", 5,
                2, "Alerta", "Sin hallazgos");
    }

    @Nested
    @DisplayName("examen vacio")
    class ExamenVacio {

        @Test
        @DisplayName("empty() nace con todos los campos en null")
        void empty_nace_con_todos_los_campos_en_null() {
            PhysicalExam vacio = PhysicalExam.empty();

            assertThat(vacio.temperature()).isNull();
            assertThat(vacio.heartRate()).isNull();
            assertThat(vacio.respiratoryRate()).isNull();
            assertThat(vacio.mucousMembranes()).isNull();
            assertThat(vacio.capillaryRefill()).isNull();
            assertThat(vacio.hydration()).isNull();
            assertThat(vacio.bodyConditionScore()).isNull();
            assertThat(vacio.painScore()).isNull();
            assertThat(vacio.attitude()).isNull();
            assertThat(vacio.findings()).isNull();
        }

        @Test
        @DisplayName("isEmpty() es verdadero solo cuando todos los campos son null")
        void is_empty_es_verdadero_solo_cuando_todos_son_null() {
            assertThat(PhysicalExam.empty().isEmpty()).isTrue();
            assertThat(completo().isEmpty()).isFalse();
        }

        @Test
        @DisplayName("un solo campo presente ya no es un examen vacio")
        void un_solo_campo_presente_ya_no_es_vacio() {
            PhysicalExam soloTemperatura = new PhysicalExam(new BigDecimal("38.0"), null, null,
                    null, null, null, null, null, null, null);

            assertThat(soloTemperatura.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("blank-to-null y recorte de texto")
    class BlankToNull {

        @Test
        @DisplayName("los campos de texto en blanco se normalizan a null")
        void los_campos_de_texto_en_blanco_se_normalizan_a_null() {
            PhysicalExam exam = new PhysicalExam(null, null, null, "   ", "", "  ", null, null,
                    "   ", "");

            assertThat(exam.mucousMembranes()).isNull();
            assertThat(exam.capillaryRefill()).isNull();
            assertThat(exam.hydration()).isNull();
            assertThat(exam.attitude()).isNull();
            assertThat(exam.findings()).isNull();
        }

        @Test
        @DisplayName("los campos de texto con contenido se recortan (strip)")
        void los_campos_de_texto_con_contenido_se_recortan() {
            PhysicalExam exam = new PhysicalExam(null, null, null, "  Rosadas  ", " < 2 seg ",
                    " Normal ", null, null, " Alerta ", " Sin hallazgos ");

            assertThat(exam.mucousMembranes()).isEqualTo("Rosadas");
            assertThat(exam.capillaryRefill()).isEqualTo("< 2 seg");
            assertThat(exam.hydration()).isEqualTo("Normal");
            assertThat(exam.attitude()).isEqualTo("Alerta");
            assertThat(exam.findings()).isEqualTo("Sin hallazgos");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("temperature negativa",
                            (ThrowingCallable) () -> new PhysicalExam(new BigDecimal("-1"), null,
                                    null, null, null, null, null, null, null, null),
                            "temperature must be between 0 and 60"),
                    arguments("temperature mayor a 60",
                            (ThrowingCallable) () -> new PhysicalExam(new BigDecimal("60.1"), null,
                                    null, null, null, null, null, null, null, null),
                            "temperature must be between 0 and 60"),
                    arguments("heartRate negativo",
                            (ThrowingCallable) () -> new PhysicalExam(null, -1, null, null, null,
                                    null, null, null, null, null),
                            "heartRate must be between 0 and 1000"),
                    arguments("heartRate mayor a 1000",
                            (ThrowingCallable) () -> new PhysicalExam(null, 1001, null, null, null,
                                    null, null, null, null, null),
                            "heartRate must be between 0 and 1000"),
                    arguments("respiratoryRate negativo",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, -1, null, null,
                                    null, null, null, null, null),
                            "respiratoryRate must be between 0 and 1000"),
                    arguments("respiratoryRate mayor a 1000",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, 1001, null, null,
                                    null, null, null, null, null),
                            "respiratoryRate must be between 0 and 1000"),
                    arguments("bodyConditionScore menor a 1",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, 0, null, null, null),
                            "bodyConditionScore must be between 1 and 9"),
                    arguments("bodyConditionScore mayor a 9",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, 10, null, null, null),
                            "bodyConditionScore must be between 1 and 9"),
                    arguments("painScore negativo",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, -1, null, null),
                            "painScore must be between 0 and 10"),
                    arguments("painScore mayor a 10",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, 11, null, null),
                            "painScore must be between 0 and 10"),
                    arguments("mucousMembranes de 41 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null,
                                    "x".repeat(41), null, null, null, null, null, null),
                            "mucousMembranes must be 40 chars or less"),
                    arguments("capillaryRefill de 21 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null,
                                    "x".repeat(21), null, null, null, null, null),
                            "capillaryRefill must be 20 chars or less"),
                    arguments("hydration de 21 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    "x".repeat(21), null, null, null, null),
                            "hydration must be 20 chars or less"),
                    arguments("attitude de 41 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, null, "x".repeat(41), null),
                            "attitude must be 40 chars or less"),
                    arguments("findings de 2001 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, null, null, "x".repeat(2001)),
                            "findings must be 2000 chars or less"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor compacto rechaza")
        void el_constructor_compacto_rechaza(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        static Stream<Arguments> casosValidosEnElLimite() {
            return Stream.of(
                    arguments("temperature 0",
                            (ThrowingCallable) () -> new PhysicalExam(BigDecimal.ZERO, null, null,
                                    null, null, null, null, null, null, null)),
                    arguments("temperature 60",
                            (ThrowingCallable) () -> new PhysicalExam(new BigDecimal("60"), null,
                                    null, null, null, null, null, null, null, null)),
                    arguments("heartRate 0",
                            (ThrowingCallable) () -> new PhysicalExam(null, 0, null, null, null,
                                    null, null, null, null, null)),
                    arguments("heartRate 1000",
                            (ThrowingCallable) () -> new PhysicalExam(null, 1000, null, null, null,
                                    null, null, null, null, null)),
                    arguments("respiratoryRate 0",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, 0, null, null,
                                    null, null, null, null, null)),
                    arguments("respiratoryRate 1000",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, 1000, null, null,
                                    null, null, null, null, null)),
                    arguments("bodyConditionScore 1",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, 1, null, null, null)),
                    arguments("bodyConditionScore 9",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, 9, null, null, null)),
                    arguments("painScore 0",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, 0, null, null)),
                    arguments("painScore 10",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, 10, null, null)),
                    arguments("mucousMembranes 40 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null,
                                    "x".repeat(40), null, null, null, null, null, null)),
                    arguments("capillaryRefill 20 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null,
                                    "x".repeat(20), null, null, null, null, null)),
                    arguments("hydration 20 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    "x".repeat(20), null, null, null, null)),
                    arguments("attitude 40 chars",
                            (ThrowingCallable) () -> new PhysicalExam(null, null, null, null, null,
                                    null, null, null, "x".repeat(40), null)),
                    arguments("findings 2000 chars", (ThrowingCallable) () -> new PhysicalExam(null,
                            null, null, null, null, null, null, null, null, "x".repeat(2000))));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosValidosEnElLimite")
        @DisplayName("el limite exacto de cada invariante se acepta")
        void el_limite_exacto_se_acepta(String caso, ThrowingCallable construccion) {
            assertThatCode(construccion).doesNotThrowAnyException();
        }
    }
}
