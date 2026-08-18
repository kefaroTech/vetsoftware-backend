package com.vetsoftware.app.surgery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Surgery")
class SurgeryTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 10);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 10, 9, 0);

    private static final SurgeryTypeRef OVARIOHISTERECTOMIA = new SurgeryTypeRef(5L,
            "Ovariohisterectomia");
    private static final AnimalRef FIRULAIS = new AnimalRef(100L, "Firulais", "A-001");
    private static final ConsultationRef CONSULTA = new ConsultationRef(200L, FECHA.minusDays(1));
    private static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Norte", "NIT-900");

    private static Surgery cirugiaValida() {
        return new Surgery(300L, FECHA, OVARIOHISTERECTOMIA, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, SurgeryStatus.PROGRAMADA, FIRULAIS,
                CONSULTA, CLINICA, CREADO, true);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("create() arranca en PROGRAMADA, habilitada y sin id")
        void create_arranca_en_programada_habilitada_y_sin_id() {
            Surgery surgery = Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Cirugia de urgencia",
                    "Ketamina", "Ninguna", null, FIRULAIS, CONSULTA, CLINICA);

            assertThat(surgery.getId()).isNull();
            assertThat(surgery.getStatus()).isEqualTo(SurgeryStatus.PROGRAMADA);
            assertThat(surgery.isEnabled()).isTrue();
            assertThat(surgery.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("create() conserva cada campo recibido")
        void create_conserva_cada_campo_recibido() {
            Surgery surgery = Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Cirugia de urgencia",
                    "Ketamina", "Ninguna", "Sangrado leve", FIRULAIS, CONSULTA, CLINICA);

            assertThat(surgery.getDate()).isEqualTo(FECHA);
            assertThat(surgery.getSurgeryType()).isEqualTo(OVARIOHISTERECTOMIA);
            assertThat(surgery.getDescription()).isEqualTo("Cirugia de urgencia");
            assertThat(surgery.getMedicament()).isEqualTo("Ketamina");
            assertThat(surgery.getObservations()).isEqualTo("Ninguna");
            assertThat(surgery.getComplications()).isEqualTo("Sangrado leve");
            assertThat(surgery.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(surgery.getConsultation()).isEqualTo(CONSULTA);
            assertThat(surgery.getCompany()).isEqualTo(CLINICA);
        }

        @Test
        @DisplayName("create() sin consulta asociada la deja en null")
        void create_sin_consulta_asociada() {
            Surgery surgery = Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Cirugia de urgencia",
                    "Ketamina", "Ninguna", null, FIRULAIS, null, CLINICA);

            assertThat(surgery.getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update() reemplaza cada campo sin tocar id, estado, alta ni fecha de creacion")
        void update_reemplaza_cada_campo_sin_tocar_id_estado_alta_ni_fecha_de_creacion() {
            Surgery surgery = cirugiaValida();
            SurgeryTypeRef castracion = new SurgeryTypeRef(6L, "Castracion");
            AnimalRef michi = new AnimalRef(101L, "Michi", "A-002");
            CompanyRef otraClinica = new CompanyRef(10L, "Clinica Sur", "NIT-901");

            surgery.update(FECHA.plusDays(5), castracion, "Castracion electiva", "Anestesia local",
                    "Observaciones nuevas", "Sangrado leve", michi, null, otraClinica);

            assertThat(surgery.getId()).isEqualTo(300L);
            assertThat(surgery.getDate()).isEqualTo(FECHA.plusDays(5));
            assertThat(surgery.getSurgeryType()).isEqualTo(castracion);
            assertThat(surgery.getDescription()).isEqualTo("Castracion electiva");
            assertThat(surgery.getMedicament()).isEqualTo("Anestesia local");
            assertThat(surgery.getObservations()).isEqualTo("Observaciones nuevas");
            assertThat(surgery.getComplications()).isEqualTo("Sangrado leve");
            assertThat(surgery.getAnimal()).isEqualTo(michi);
            assertThat(surgery.getConsultation()).isNull();
            assertThat(surgery.getCompany()).isEqualTo(otraClinica);
            assertThat(surgery.getStatus()).isEqualTo(SurgeryStatus.PROGRAMADA);
            assertThat(surgery.getCreatedDate()).isEqualTo(CREADO);
            assertThat(surgery.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cambio de estado")
    class CambioDeEstado {

        @Test
        @DisplayName("un estado nulo se rechaza")
        void un_estado_nulo_se_rechaza() {
            Surgery surgery = cirugiaValida();

            assertThatThrownBy(() -> surgery.changeStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }

        @ParameterizedTest
        @EnumSource(SurgeryStatus.class)
        @DisplayName("cualquier estado del catalogo se acepta sin importar el estado actual: no hay"
                + " matriz de transiciones en el dominio")
        void cualquier_estado_del_catalogo_se_acepta(SurgeryStatus nuevoEstado) {
            Surgery surgery = cirugiaValida();

            surgery.changeStatus(nuevoEstado);

            assertThat(surgery.getStatus()).isEqualTo(nuevoEstado);
        }
    }

    @Nested
    @DisplayName("Validaciones de invariantes")
    class Validaciones {

        @Test
        @DisplayName("una fecha nula se rechaza")
        void una_fecha_nula_se_rechaza() {
            assertThatThrownBy(() -> Surgery.create(null, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, null, FIRULAIS, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("date is required");
        }

        @Test
        @DisplayName("un tipo de cirugia nulo se rechaza")
        void un_tipo_de_cirugia_nulo_se_rechaza() {
            assertThatThrownBy(() -> Surgery.create(FECHA, null, "Descripcion", null, null, null,
                    FIRULAIS, null, CLINICA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("surgeryType is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("una descripcion en blanco se rechaza")
        void una_descripcion_en_blanco_se_rechaza(String descripcion) {
            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, descripcion, null,
                    null, null, FIRULAIS, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        @DisplayName("una descripcion de mas de 2000 caracteres se rechaza")
        void una_descripcion_demasiado_larga_se_rechaza() {
            String descripcion = "x".repeat(2001);

            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, descripcion, null,
                    null, null, FIRULAIS, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description must be 2000 chars or less");
        }

        @Test
        @DisplayName("el medicamento es opcional")
        void el_medicamento_es_opcional() {
            assertThatCode(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, null, FIRULAIS, null, CLINICA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un medicamento de mas de 200 caracteres se rechaza")
        void un_medicamento_demasiado_largo_se_rechaza() {
            String medicamento = "x".repeat(201);

            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion",
                    medicamento, null, null, FIRULAIS, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("medicament must be 200 chars or less");
        }

        @Test
        @DisplayName("las observaciones son opcionales")
        void las_observaciones_son_opcionales() {
            assertThatCode(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, null, FIRULAIS, null, CLINICA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("unas observaciones de mas de 2000 caracteres se rechazan")
        void unas_observaciones_demasiado_largas_se_rechazan() {
            String observaciones = "x".repeat(2001);

            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    observaciones, null, FIRULAIS, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("observations must be 2000 chars or less");
        }

        @Test
        @DisplayName("las complicaciones son opcionales")
        void las_complicaciones_son_opcionales() {
            assertThatCode(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, null, FIRULAIS, null, CLINICA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("unas complicaciones de mas de 2000 caracteres se rechazan")
        void unas_complicaciones_demasiado_largas_se_rechazan() {
            String complicaciones = "x".repeat(2001);

            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, complicaciones, FIRULAIS, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("complications must be 2000 chars or less");
        }

        @Test
        @DisplayName("un estado nulo en el constructor publico se rechaza: solo alcanzable llamando"
                + " al constructor directamente, create() siempre pasa PROGRAMADA")
        void un_estado_nulo_en_el_constructor_publico_se_rechaza() {
            assertThatThrownBy(() -> new Surgery(null, FECHA, OVARIOHISTERECTOMIA, "Descripcion",
                    null, null, null, null, FIRULAIS, null, CLINICA, CREADO, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }

        @Test
        @DisplayName("un animal nulo se rechaza")
        void un_animal_nulo_se_rechaza() {
            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, null, null, null, CLINICA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal is required");
        }

        @Test
        @DisplayName("una empresa nula se rechaza")
        void una_empresa_nula_se_rechaza() {
            assertThatThrownBy(() -> Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Descripcion", null,
                    null, null, FIRULAIS, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @Test
        @DisplayName("update() vuelve a ejercitar las mismas invariantes que create()")
        void update_vuelve_a_ejercitar_las_mismas_invariantes() {
            Surgery surgery = cirugiaValida();

            assertThatThrownBy(() -> surgery.update(FECHA, OVARIOHISTERECTOMIA, "  ", null, null,
                    null, FIRULAIS, null, CLINICA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }
    }

    @Nested
    @DisplayName("Habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable() la deshabilita y enable() la vuelve a habilitar")
        void disable_la_deshabilita_y_enable_la_vuelve_a_habilitar() {
            Surgery surgery = cirugiaValida();

            surgery.disable();
            assertThat(surgery.isEnabled()).isFalse();

            surgery.enable();
            assertThat(surgery.isEnabled()).isTrue();
        }
    }
}
