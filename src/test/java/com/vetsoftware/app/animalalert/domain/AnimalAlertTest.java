package com.vetsoftware.app.animalalert.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las invariantes de la alerta viven en el constructor, no en el service: una
 * alerta sin descripcion o sin animal es un dato que ningun caller deberia
 * poder producir, y el constructor es el unico sitio donde eso se garantiza de
 * verdad.
 */
@DisplayName("AnimalAlert")
class AnimalAlertTest {

    @Nested
    @DisplayName("construccion — invariantes del constructor")
    class Construccion {

        @Test
        @DisplayName("con datos validos construye la alerta y expone cada campo")
        void con_datos_validos_construye_la_alerta() {
            AnimalAlert alert = AnimalAlertMother.alergia();

            assertThat(alert.getId()).isEqualTo(AnimalAlertMother.ALERT_ID);
            assertThat(alert.getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
            assertThat(alert.getCompany()).isEqualTo(AnimalAlertMother.CLINICA);
            assertThat(alert.getType()).isEqualTo(AlertType.ALLERGY);
            assertThat(alert.getDescription()).isEqualTo("Alergia a la penicilina");
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(alert.getCreatedDate()).isEqualTo(AnimalAlertMother.CREADO);
            assertThat(alert.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la severidad es opcional: null no rompe la construccion")
        void la_severidad_es_opcional() {
            assertThatCode(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.FIRULAIS, AnimalAlertMother.CLINICA, AlertType.OTHER,
                    "Sin severidad asignada", null, AnimalAlertMother.CREADO, null, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza type null")
        void rechaza_type_null() {
            assertThatThrownBy(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.FIRULAIS, AnimalAlertMother.CLINICA, null, "Descripcion",
                    AlertSeverity.LOW, AnimalAlertMother.CREADO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza descripcion vacia")
        void rechaza_descripcion_vacia(String descripcion) {
            assertThatThrownBy(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.FIRULAIS, AnimalAlertMother.CLINICA, AlertType.ALLERGY,
                    descripcion, AlertSeverity.LOW, AnimalAlertMother.CREADO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        @DisplayName("rechaza descripcion de mas de 255 caracteres")
        void rechaza_descripcion_muy_larga() {
            String muyLarga = "x".repeat(256);

            assertThatThrownBy(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.FIRULAIS, AnimalAlertMother.CLINICA, AlertType.ALLERGY,
                    muyLarga, AlertSeverity.LOW, AnimalAlertMother.CREADO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description must be 255 chars or less");
        }

        @Test
        @DisplayName("una descripcion de exactamente 255 caracteres si se acepta")
        void una_descripcion_de_255_caracteres_se_acepta() {
            String limite = "x".repeat(255);

            assertThatCode(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.FIRULAIS, AnimalAlertMother.CLINICA, AlertType.ALLERGY,
                    limite, AlertSeverity.LOW, AnimalAlertMother.CREADO, null, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza animal null")
        void rechaza_animal_null() {
            assertThatThrownBy(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID, null,
                    AnimalAlertMother.CLINICA, AlertType.ALLERGY, "Descripcion", AlertSeverity.LOW,
                    AnimalAlertMother.CREADO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal is required");
        }

        @Test
        @DisplayName("rechaza company null")
        void rechaza_company_null() {
            assertThatThrownBy(() -> new AnimalAlert(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.FIRULAIS, null, AlertType.ALLERGY, "Descripcion",
                    AlertSeverity.LOW, AnimalAlertMother.CREADO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }
    }

    @Nested
    @DisplayName("create — factory de alta")
    class Create {

        @Test
        @DisplayName("nace sin id, habilitada y con fecha de creacion asignada")
        void nace_sin_id_habilitada_y_con_fecha() {
            AnimalAlert alert = AnimalAlert.create(AnimalAlertMother.FIRULAIS, AlertType.BEHAVIOR,
                    "Agresivo con extranos", AlertSeverity.MEDIUM, AnimalAlertMother.CLINICA);

            assertThat(alert.getId()).isNull();
            assertThat(alert.isEnabled()).isTrue();
            assertThat(alert.getCreatedDate()).isNotNull();
            assertThat(alert.getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
            assertThat(alert.getCompany()).isEqualTo(AnimalAlertMother.CLINICA);
        }

        @Test
        @DisplayName("tambien valida sus invariantes: una descripcion vacia no crea nada")
        void tambien_valida_sus_invariantes() {
            assertThatThrownBy(() -> AnimalAlert.create(AnimalAlertMother.FIRULAIS,
                    AlertType.BEHAVIOR, "  ", AlertSeverity.MEDIUM, AnimalAlertMother.CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }
    }

    @Nested
    @DisplayName("update — mutacion")
    class Update {

        @Test
        @DisplayName("cambia tipo, descripcion y severidad sin tocar animal ni empresa")
        void cambia_tipo_descripcion_y_severidad() {
            AnimalAlert alert = AnimalAlertMother.alergia();

            alert.update(AlertType.BEHAVIOR, "Agresivo con extranos", AlertSeverity.LOW);

            assertThat(alert.getType()).isEqualTo(AlertType.BEHAVIOR);
            assertThat(alert.getDescription()).isEqualTo("Agresivo con extranos");
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.LOW);
            assertThat(alert.getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
            assertThat(alert.getCompany()).isEqualTo(AnimalAlertMother.CLINICA);
        }

        @Test
        @DisplayName("tambien revalida: un tipo null lo rechaza sin mutar nada")
        void tambien_revalida_al_actualizar() {
            AnimalAlert alert = AnimalAlertMother.alergia();

            assertThatThrownBy(() -> alert.update(null, "Nueva descripcion", AlertSeverity.LOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type is required");

            assertThat(alert.getType()).isEqualTo(AlertType.ALLERGY);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable apaga la alerta")
        void disable_apaga_la_alerta() {
            AnimalAlert alert = AnimalAlertMother.alergia();

            alert.disable();

            assertThat(alert.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable la vuelve a encender")
        void enable_la_vuelve_a_encender() {
            AnimalAlert alert = AnimalAlertMother.deshabilitada();

            alert.enable();

            assertThat(alert.isEnabled()).isTrue();
        }
    }

    @Test
    @DisplayName("createdDate se conserva tal cual — no se recalcula en cada acceso")
    void created_date_se_conserva_tal_cual() {
        LocalDateTime fecha = LocalDateTime.of(2020, 1, 1, 0, 0);
        AnimalAlert alert = new AnimalAlert(1L, AnimalAlertMother.FIRULAIS,
                AnimalAlertMother.CLINICA, AlertType.OTHER, "Otra alerta", AlertSeverity.LOW, fecha,
                null, true);

        assertThat(alert.getCreatedDate()).isEqualTo(fecha);
    }
}
