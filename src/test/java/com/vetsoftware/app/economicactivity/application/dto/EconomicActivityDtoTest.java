package com.vetsoftware.app.economicactivity.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EconomicActivityDto.from")
class EconomicActivityDtoTest {

    @Test
    @DisplayName("traslada cada campo de una actividad habilitada")
    void traslada_cada_campo_de_una_actividad_habilitada() {
        EconomicActivityDto dto = EconomicActivityDto.from(EconomicActivityMother.existente());

        assertThat(dto.id()).isEqualTo(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
        assertThat(dto.code()).isEqualTo(EconomicActivityMother.CODIGO);
        assertThat(dto.name()).isEqualTo(EconomicActivityMother.NOMBRE);
        assertThat(dto.createdDate()).isEqualTo(EconomicActivityMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("traslada el estado deshabilitado")
    void traslada_el_estado_deshabilitado() {
        EconomicActivityDto dto = EconomicActivityDto.from(EconomicActivityMother.deshabilitada());

        assertThat(dto.enabled()).isFalse();
    }
}
