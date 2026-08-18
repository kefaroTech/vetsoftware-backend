package com.vetsoftware.app.permission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.permission.testsupport.PermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionDto")
class PermissionDtoTest {

    @Test
    @DisplayName("from() copia cada campo del permiso, incluidas las referencias")
    void from_copia_cada_campo() {
        PermissionDto dto = PermissionDto.from(PermissionMother.permisoValido());

        assertThat(dto.id()).isEqualTo(PermissionMother.PERMISSION_ID);
        assertThat(dto.name()).isEqualTo("Crear factura");
        assertThat(dto.code()).isEqualTo("billing.create");
        assertThat(dto.company().id()).isEqualTo(PermissionMother.COMPANY_ID);
        assertThat(dto.company().name()).isEqualTo(PermissionMother.CLINICA.name());
        assertThat(dto.subModule().id()).isEqualTo(PermissionMother.SUB_MODULE_ID);
        assertThat(dto.createdDate()).isEqualTo(PermissionMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() refleja un permiso deshabilitado")
    void from_refleja_un_permiso_deshabilitado() {
        PermissionDto dto = PermissionDto.from(PermissionMother.deshabilitado());

        assertThat(dto.enabled()).isFalse();
    }
}
