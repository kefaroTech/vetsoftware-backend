package com.vetsoftware.app.systempermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import com.vetsoftware.app.systempermission.testsupport.SystemPermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemPermissionDto.from")
class SystemPermissionDtoTest {

    @Test
    @DisplayName("mapea cada campo del permiso, campo por campo")
    void mapea_cada_campo() {
        SystemPermission permission = SystemPermissionMother.permisoValido();

        SystemPermissionDto dto = SystemPermissionDto.from(permission);

        assertThat(dto.id()).isEqualTo(permission.getId());
        assertThat(dto.name()).isEqualTo(permission.getName());
        assertThat(dto.code()).isEqualTo(permission.getCode());
        assertThat(dto.createdDate()).isEqualTo(permission.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(permission.isEnabled());
    }

    @Test
    @DisplayName("un permiso deshabilitado mapea enabled en false")
    void un_permiso_deshabilitado_mapea_enabled_en_false() {
        SystemPermission permission = SystemPermissionMother.permisoValido();
        permission.disable();

        assertThat(SystemPermissionDto.from(permission).enabled()).isFalse();
    }
}
