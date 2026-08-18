package com.vetsoftware.app.systemconfiguration.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import com.vetsoftware.app.systemconfiguration.testsupport.SystemConfigurationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemConfigurationDto.from")
class SystemConfigurationDtoTest {

    @Test
    @DisplayName("copia cada campo de la configuracion en su posicion")
    void copia_cada_campo_en_su_posicion() {
        SystemConfigurationDto dto = SystemConfigurationDto
                .from(SystemConfigurationMother.configuracionExistente());

        assertThat(dto.id()).isEqualTo(SystemConfigurationMother.CONFIG_ID);
        assertThat(dto.propertyName()).isEqualTo(SystemConfigurationMother.PROPERTY_NAME);
        assertThat(dto.value()).isEqualTo(SystemConfigurationMother.VALUE);
        assertThat(dto.createdDate()).isEqualTo(SystemConfigurationMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("propaga una configuracion deshabilitada")
    void propaga_una_configuracion_deshabilitada() {
        SystemConfiguration config = SystemConfigurationMother.configuracionExistente();
        config.disable();

        assertThat(SystemConfigurationDto.from(config).enabled()).isFalse();
    }
}
