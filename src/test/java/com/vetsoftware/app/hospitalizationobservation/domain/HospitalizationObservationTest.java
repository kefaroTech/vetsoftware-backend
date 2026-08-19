package com.vetsoftware.app.hospitalizationobservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HospitalizationObservation — invariantes de la observacion de hospitalizacion")
class HospitalizationObservationTest {

    private static final EmployeeRef VETERINARIO = HospitalizationObservationMother.VETERINARIO;
    private static final HospitalizationRef HOSPITALIZACION = HospitalizationObservationMother.HOSPITALIZACION;

    private static HospitalizationObservation construir(String description,
            HospitalizationRef hospitalization, EmployeeRef createdBy) {
        return new HospitalizationObservation(HospitalizationObservationMother.OBSERVATION_ID,
                description, hospitalization, createdBy, HospitalizationObservationMother.CREADO,
                null, true);
    }

    @Nested
    @DisplayName("construccion valida")
    class ConstruccionValida {

        @Test
        @DisplayName("conserva cada campo tal como se paso")
        void conserva_cada_campo() {
            HospitalizationObservation observation = HospitalizationObservationMother
                    .observacionValida();

            assertThat(observation.getId())
                    .isEqualTo(HospitalizationObservationMother.OBSERVATION_ID);
            assertThat(observation.getDescription())
                    .isEqualTo(HospitalizationObservationMother.DESCRIPCION);
            assertThat(observation.getHospitalization()).isEqualTo(HOSPITALIZACION);
            assertThat(observation.getCreatedBy()).isEqualTo(VETERINARIO);
            assertThat(observation.getCreatedDate())
                    .isEqualTo(HospitalizationObservationMother.CREADO);
            assertThat(observation.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("crea sin id todavia, habilitada y con la fecha de creacion asignada")
        void crea_sin_id_habilitada_y_con_fecha_asignada() {
            HospitalizationObservation observation = HospitalizationObservation
                    .create("Paciente estable", HOSPITALIZACION, VETERINARIO);

            assertThat(observation.getId()).isNull();
            assertThat(observation.getDescription()).isEqualTo("Paciente estable");
            assertThat(observation.isEnabled()).isTrue();
            assertThat(observation.getCreatedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("reemplaza la descripcion sin tocar las demas propiedades")
        void reemplaza_la_descripcion() {
            HospitalizationObservation observation = HospitalizationObservationMother
                    .observacionValida();

            observation.update("Nueva evolucion clinica");

            assertThat(observation.getDescription()).isEqualTo("Nueva evolucion clinica");
            assertThat(observation.getHospitalization()).isEqualTo(HOSPITALIZACION);
            assertThat(observation.getCreatedBy()).isEqualTo(VETERINARIO);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("con una descripcion nula o en blanco se rechaza")
        void con_descripcion_nula_o_en_blanco_se_rechaza(String valor) {
            HospitalizationObservation observation = HospitalizationObservationMother
                    .observacionValida();

            assertThatThrownBy(() -> observation.update(valor))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }
    }

    @Nested
    @DisplayName("enable() / disable()")
    class EnableDisable {

        @Test
        @DisplayName("disable apaga la observacion y enable la vuelve a encender")
        void disable_apaga_y_enable_enciende() {
            HospitalizationObservation observation = HospitalizationObservationMother
                    .observacionValida();

            observation.disable();
            assertThat(observation.isEnabled()).isFalse();

            observation.enable();
            assertThat(observation.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("invariantes de description")
    class InvariantesDescription {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nula o en blanco se rechaza")
        void nula_o_en_blanco_se_rechaza(String valor) {
            assertThatThrownBy(() -> construir(valor, HOSPITALIZACION, VETERINARIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        @DisplayName("de mas de 2000 caracteres se rechaza")
        void demasiado_larga_se_rechaza() {
            String larga = "a".repeat(2001);

            assertThatThrownBy(() -> construir(larga, HOSPITALIZACION, VETERINARIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description must be 2000 chars or less");
        }

        @Test
        @DisplayName("de exactamente 2000 caracteres se acepta")
        void exactamente_2000_se_acepta() {
            String limite = "a".repeat(2000);

            HospitalizationObservation observation = construir(limite, HOSPITALIZACION,
                    VETERINARIO);

            assertThat(observation.getDescription()).hasSize(2000);
        }
    }

    @Nested
    @DisplayName("invariantes de las referencias")
    class InvariantesReferencias {

        @Test
        @DisplayName("hospitalization nula se rechaza")
        void hospitalization_nula_se_rechaza() {
            assertThatThrownBy(() -> construir(HospitalizationObservationMother.DESCRIPCION, null,
                    VETERINARIO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hospitalization is required");
        }

        @Test
        @DisplayName("createdBy nulo se rechaza")
        void createdBy_nulo_se_rechaza() {
            assertThatThrownBy(() -> construir(HospitalizationObservationMother.DESCRIPCION,
                    HOSPITALIZACION, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdBy is required");
        }
    }

    @Test
    @DisplayName("LocalDateTime.now() no se afirma directamente: create() solo garantiza que hay fecha")
    void no_afirma_now_directamente() {
        LocalDateTime antes = LocalDateTime.now();
        HospitalizationObservation observation = HospitalizationObservation.create("x",
                HOSPITALIZACION, VETERINARIO);
        LocalDateTime despues = LocalDateTime.now();

        assertThat(observation.getCreatedDate()).isBetween(antes, despues);
    }
}
