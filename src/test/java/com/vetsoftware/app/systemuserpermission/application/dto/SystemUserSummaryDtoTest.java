package com.vetsoftware.app.systemuserpermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserSummaryDto.from")
class SystemUserSummaryDtoTest {

    @Test
    @DisplayName("copia id y code del ref")
    void copia_id_y_code_del_ref() {
        SystemUserSummaryDto dto = SystemUserSummaryDto.from(SystemUserPermissionMother.USUARIO);

        assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.USUARIO.id());
        assertThat(dto.code()).isEqualTo(SystemUserPermissionMother.USUARIO.code());
    }
}
