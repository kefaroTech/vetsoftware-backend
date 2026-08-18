package com.vetsoftware.app.systemuserpermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemPermissionSummaryDto.from")
class SystemPermissionSummaryDtoTest {

    @Test
    @DisplayName("copia id, name y code del ref")
    void copia_id_name_y_code_del_ref() {
        SystemPermissionSummaryDto dto = SystemPermissionSummaryDto
                .from(SystemUserPermissionMother.PERMISO);

        assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.PERMISO.id());
        assertThat(dto.name()).isEqualTo(SystemUserPermissionMother.PERMISO.name());
        assertThat(dto.code()).isEqualTo(SystemUserPermissionMother.PERMISO.code());
    }
}
