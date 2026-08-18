package com.vetsoftware.app.animalalert.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre
 * {@code animalId}/{@code animalName} compila, pasa cualquier test de "no es
 * null", y solo se ve en pantalla.
 */
@DisplayName("AnimalAlertDto.from")
class AnimalAlertDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        AnimalAlert alert = AnimalAlertMother.alergia();

        AnimalAlertDto dto = AnimalAlertDto.from(alert);

        assertThat(dto.id()).isEqualTo(AnimalAlertMother.ALERT_ID);
        assertThat(dto.animalId()).isEqualTo(AnimalAlertMother.ANIMAL_ID);
        assertThat(dto.animalName()).isEqualTo("Firulais");
        assertThat(dto.type()).isEqualTo(AlertType.ALLERGY);
        assertThat(dto.description()).isEqualTo("Alergia a la penicilina");
        assertThat(dto.severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(dto.createdDate()).isEqualTo(AnimalAlertMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana el animal en id y nombre sin exponer el resto del ref")
    void aplana_el_animal_en_id_y_nombre() {
        AnimalAlertDto dto = AnimalAlertDto.from(AnimalAlertMother.alergia());

        assertThat(dto.animalId()).isEqualTo(AnimalAlertMother.FIRULAIS.id());
        assertThat(dto.animalName()).isEqualTo(AnimalAlertMother.FIRULAIS.name());
    }

    @Test
    @DisplayName("propaga la alerta deshabilitada")
    void propaga_la_alerta_deshabilitada() {
        assertThat(AnimalAlertDto.from(AnimalAlertMother.deshabilitada()).enabled()).isFalse();
    }
}
