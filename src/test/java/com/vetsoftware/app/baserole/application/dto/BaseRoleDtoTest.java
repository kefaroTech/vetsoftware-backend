package com.vetsoftware.app.baserole.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserole.testsupport.BaseRoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRoleDto")
class BaseRoleDtoTest {

    @Test
    @DisplayName("from() copia cada campo del agregado")
    void from_copia_cada_campo_del_agregado() {
        BaseRoleDto dto = BaseRoleDto.from(BaseRoleMother.veterinario());

        assertThat(dto.id()).isEqualTo(BaseRoleMother.BASE_ROLE_ID);
        assertThat(dto.name()).isEqualTo("Veterinario");
        assertThat(dto.code()).isEqualTo("VET");
        assertThat(dto.mandatory()).isFalse();
        assertThat(dto.createdDate()).isEqualTo(BaseRoleMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }
}
