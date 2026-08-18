package com.vetsoftware.app.companysettings.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companysettings.testsupport.CompanySettingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySettingDto.from")
class CompanySettingDtoTest {

    @Test
    @DisplayName("copia propertyName y value")
    void copia_property_name_y_value() {
        CompanySettingDto dto = CompanySettingDto.from(CompanySettingMother.ajusteExistente());

        assertThat(dto.propertyName()).isEqualTo(CompanySettingMother.PROPERTY_NAME);
        assertThat(dto.value()).isEqualTo(CompanySettingMother.VALUE);
    }

    @Test
    @DisplayName("refleja un value distinto sin arrastrar el anterior")
    void refleja_un_value_distinto() {
        CompanySettingDto dto = CompanySettingDto
                .from(CompanySettingMother.ajusteExistente(1L, 9L, "otra.propiedad", "42"));

        assertThat(dto.propertyName()).isEqualTo("otra.propiedad");
        assertThat(dto.value()).isEqualTo("42");
    }
}
