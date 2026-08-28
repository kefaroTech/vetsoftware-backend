package com.vetsoftware.app.limitdimension.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LimitDimension — el catálogo de cosas limitables")
class LimitDimensionTest {

    private static final LocalDate ENERO = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 1, 8, 0);

    @Nested
    @DisplayName("R-LIMIT-03 · el enfriamiento es de los acumulativos y solo de ellos")
    class Enfriamiento {

        @Test
        @DisplayName("un eje acumulativo sin días de enfriamiento se rechaza")
        void un_eje_acumulativo_sin_dias_de_enfriamiento_se_rechaza() {
            assertThatThrownBy(() -> LimitDimension.create("ANIMAL", "Mascotas",
                    MeasureKind.CUMULATIVE, null, null, ENERO, CREADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required for a CUMULATIVE");
        }

        @Test
        @DisplayName("un eje de stock con días de enfriamiento se rechaza")
        void un_eje_de_stock_con_dias_de_enfriamiento_se_rechaza() {
            assertThatThrownBy(() -> LimitDimension.create("USER", "Usuarios", MeasureKind.STOCK,
                    null, 30, ENERO, CREADO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only applies to a CUMULATIVE");
        }

        @Test
        @DisplayName("el eje de mascotas libera la plaza a los treinta días, y son columna")
        void borrar_una_mascota_libera_la_plaza_a_los_treinta_dias() {
            LimitDimension animal = LimitDimension.create("ANIMAL", "Mascotas",
                    MeasureKind.CUMULATIVE, null, 30, ENERO, CREADO);

            assertThat(animal.getReleaseDelayDays()).isEqualTo(30);
            assertThat(animal.getMeasureKind().requiresReleaseDelay()).isTrue();
        }
    }

    @Nested
    @DisplayName("D-74 · desde cuándo existe el eje")
    class DesdeCuandoExiste {

        @Test
        @DisplayName("un eje de enero ya existía cuando se firmó en marzo: la ausencia de fila es"
                + " techo cero")
        void un_eje_de_enero_ya_existia_al_firmar_en_marzo() {
            LimitDimension animal = LimitDimension.create("ANIMAL", "Mascotas",
                    MeasureKind.CUMULATIVE, null, 30, ENERO, CREADO);

            assertThat(animal.existedOn(LocalDate.of(2026, 3, 1))).isTrue();
        }

        @Test
        @DisplayName("un eje de abril no existía cuando se firmó en enero: no puede bloquear")
        void un_eje_de_abril_no_existia_al_firmar_en_enero() {
            LimitDimension citas = LimitDimension.create("APPOINTMENT", "Citas", MeasureKind.FLOW,
                    null, null, LocalDate.of(2026, 4, 1), CREADO);

            assertThat(citas.existedOn(LocalDate.of(2026, 1, 15))).isFalse();
        }

        @Test
        @DisplayName("un eje sin fecha de disponibilidad se rechaza: sin ella los dos casos no se"
                + " distinguen")
        void un_eje_sin_fecha_de_disponibilidad_se_rechaza() {
            assertThatThrownBy(() -> LimitDimension.create("APPOINTMENT", "Citas", MeasureKind.FLOW,
                    null, null, null, CREADO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("available from");
        }
    }

    @Nested
    @DisplayName("R-LIMIT-22 · el tipo de medida no se cambia desde el dominio")
    class TipoDeMedida {

        @Test
        @DisplayName("actualizar un eje conserva su tipo de medida")
        void actualizar_un_eje_conserva_su_tipo_de_medida() {
            LimitDimension animal = LimitDimension.create("ANIMAL", "Mascotas",
                    MeasureKind.CUMULATIVE, null, 30, ENERO, CREADO);

            animal.update("Mascotas registradas históricamente",
                    new SubModuleRef(4L, "CLINICAL_HISTORY", "Historia clínica"), 45);

            assertThat(animal.getMeasureKind()).isEqualTo(MeasureKind.CUMULATIVE);
            assertThat(animal.getReleaseDelayDays()).isEqualTo(45);
            assertThat(animal.getSubModule().code()).isEqualTo("CLINICAL_HISTORY");
        }

        @Test
        @DisplayName("los tres tipos declaran qué exigen y qué admiten")
        void los_tres_tipos_declaran_que_exigen() {
            assertThat(MeasureKind.CUMULATIVE.requiresReleaseDelay()).isTrue();
            assertThat(MeasureKind.CUMULATIVE.admitsOverage()).isFalse();
            assertThat(MeasureKind.FLOW.requiresResetPeriod()).isTrue();
            assertThat(MeasureKind.FLOW.admitsOverage()).isTrue();
            assertThat(MeasureKind.STOCK.requiresReleaseDelay()).isFalse();
            assertThat(MeasureKind.STOCK.requiresResetPeriod()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un eje sin código se rechaza")
        void un_eje_sin_codigo_se_rechaza() {
            assertThatThrownBy(() -> LimitDimension.create("  ", "Mascotas", MeasureKind.STOCK,
                    null, null, ENERO, CREADO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("un submódulo sin código se rechaza")
        void un_submodulo_sin_codigo_se_rechaza() {
            assertThatThrownBy(() -> new SubModuleRef(4L, null, "Historia clínica"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
