package com.vetsoftware.app.withholdingconfig.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.withholdingconfig.testsupport.WithholdingConfigMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code WithholdingConfig} valida en su constructor que {@code company} no sea
 * nulo, así que la rama {@code company == null} del {@code from(...)} de este
 * DTO es defensiva pero inalcanzable sin reflexión: ningún
 * {@code WithholdingConfig} real puede llegar aquí con company nula. No se
 * fuerza con reflexión — queda documentada como hueco en el informe final.
 */
@DisplayName("WithholdingConfigDto — from()")
class WithholdingConfigDtoTest {

    @Test
    @DisplayName("mapea todos los campos, incluida la company")
    void mapea_todos_los_campos() {
        WithholdingConfigDto dto = WithholdingConfigDto
                .from(WithholdingConfigMother.configValida());

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.companyId()).isEqualTo(WithholdingConfigMother.COMPANY_ID);
        assertThat(dto.reteFuenteRate()).isEqualByComparingTo("2.5");
        assertThat(dto.reteIvaRate()).isEqualByComparingTo("15.0");
        assertThat(dto.reteIcaRate()).isEqualByComparingTo("1.0");
        assertThat(dto.createdDate()).isEqualTo(WithholdingConfigMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("propaga el estado deshabilitado")
    void propaga_el_estado_deshabilitado() {
        WithholdingConfigDto dto = WithholdingConfigDto
                .from(WithholdingConfigMother.deshabilitada());

        assertThat(dto.enabled()).isFalse();
    }
}
