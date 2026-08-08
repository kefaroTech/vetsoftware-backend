package com.vetsoftware.app.animal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("WeightRecord — punto de la serie temporal de peso")
class WeightRecordTest {

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private AnimalRef animal = WeightRecordMother.FIRULAIS;
        private BigDecimal value = new BigDecimal("12.50");
        private WeightType unit = WeightType.KILOGRAMS;
        private LocalDate measuredAt = WeightRecordMother.MEDIDO_EL;
        private WeightSource source = WeightSource.MANUAL;
        private Long sourceId;
        private String note = "control de rutina";
        private CompanyRef company = AnimalMother.CLINICA;

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder value(BigDecimal v) {
            this.value = v;
            return this;
        }

        private Builder unit(WeightType v) {
            this.unit = v;
            return this;
        }

        private Builder measuredAt(LocalDate v) {
            this.measuredAt = v;
            return this;
        }

        private Builder source(WeightSource v) {
            this.source = v;
            return this;
        }

        private Builder sourceId(Long v) {
            this.sourceId = v;
            return this;
        }

        private Builder note(String v) {
            this.note = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private WeightRecord build() {
            return new WeightRecord(1L, animal, value, unit, measuredAt, source, sourceId, note,
                    company, WeightRecordMother.CREADO, true);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo en su sitio")
        void conserva_cada_campo_en_su_sitio() {
            WeightRecord record = valido().build();

            assertThat(record.getId()).isEqualTo(1L);
            assertThat(record.getAnimal()).isEqualTo(WeightRecordMother.FIRULAIS);
            assertThat(record.getValue()).isEqualByComparingTo("12.50");
            assertThat(record.getUnit()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(record.getMeasuredAt()).isEqualTo(WeightRecordMother.MEDIDO_EL);
            assertThat(record.getSource()).isEqualTo(WeightSource.MANUAL);
            assertThat(record.getSourceId()).isNull();
            assertThat(record.getNote()).isEqualTo("control de rutina");
            assertThat(record.getCompany()).isEqualTo(AnimalMother.CLINICA);
            assertThat(record.getCreatedDate()).isEqualTo(WeightRecordMother.CREADO);
            assertThat(record.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            WeightRecord record = WeightRecord.create(WeightRecordMother.FIRULAIS,
                    new BigDecimal("12.50"), WeightType.KILOGRAMS, LocalDate.now(),
                    WeightSource.MANUAL, null, null, AnimalMother.CLINICA);

            assertThat(record.getId()).isNull();
            assertThat(record.isEnabled()).isTrue();
            assertThat(record.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("la nota es opcional")
        void la_nota_es_opcional() {
            assertThatCode(() -> valido().note(null).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("animal null", (ThrowingCallable) () -> valido().animal(null).build(),
                            "animal is required"),
                    arguments("value null", (ThrowingCallable) () -> valido().value(null).build(),
                            "value is required"),
                    arguments("value cero",
                            (ThrowingCallable) () -> valido().value(BigDecimal.ZERO).build(),
                            "value must be greater than zero"),
                    arguments("value negativo",
                            (ThrowingCallable) () -> valido().value(new BigDecimal("-0.01"))
                                    .build(),
                            "value must be greater than zero"),
                    arguments("unit null", (ThrowingCallable) () -> valido().unit(null).build(),
                            "unit is required"),
                    arguments("measuredAt null",
                            (ThrowingCallable) () -> valido().measuredAt(null).build(),
                            "measuredAt is required"),
                    arguments("measuredAt en el futuro",
                            (ThrowingCallable) () -> valido()
                                    .measuredAt(LocalDate.now().plusDays(1)).build(),
                            "measuredAt cannot be in the future"),
                    arguments("source null", (ThrowingCallable) () -> valido().source(null).build(),
                            "source is required"),
                    arguments("note de 501 chars",
                            (ThrowingCallable) () -> valido().note("x".repeat(501)).build(),
                            "note must be 500 chars or less"),
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("medir hoy es valido: el corte es estrictamente el futuro")
        void medir_hoy_es_valido() {
            assertThatCode(() -> valido().measuredAt(LocalDate.now()).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {0, 500})
        @DisplayName("note en el limite exacto se acepta")
        void note_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().note("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un valor positivo minusculo se acepta: la regla es signo, no magnitud")
        void un_valor_positivo_minusculo_se_acepta() {
            assertThatCode(() -> valido().value(new BigDecimal("0.001")).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("coherencia entre source y sourceId")
    class CoherenciaDeOrigen {

        @ParameterizedTest(name = "source {0} exige sourceId")
        @EnumSource(value = WeightSource.class, names = "MANUAL", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("un origen clinico sin sourceId se rechaza")
        void un_origen_clinico_sin_source_id_se_rechaza(WeightSource source) {
            assertThatThrownBy(() -> valido().source(source).sourceId(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceId is required when source is " + source);
        }

        @ParameterizedTest(name = "source {0} con sourceId")
        @EnumSource(value = WeightSource.class, names = "MANUAL", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("un origen clinico con sourceId se acepta")
        void un_origen_clinico_con_source_id_se_acepta(WeightSource source) {
            assertThatCode(() -> valido().source(source).sourceId(77L).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MANUAL con sourceId se rechaza: no cuelga de ningun evento")
        void manual_con_source_id_se_rechaza() {
            assertThatThrownBy(() -> valido().source(WeightSource.MANUAL).sourceId(77L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceId must be null for MANUAL records");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado")
        void disable_y_enable_alternan_el_estado() {
            WeightRecord record = valido().build();

            record.disable();
            assertThat(record.isEnabled()).isFalse();

            record.enable();
            assertThat(record.isEnabled()).isTrue();
        }
    }
}
