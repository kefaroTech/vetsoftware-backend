package com.vetsoftware.app.publishadminpermissions.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.publishadminpermissions.domain.PublishAdminPermissionsResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PublishAdminPermissionsDto — proyeccion del resultado de dominio")
class PublishAdminPermissionsDtoTest {

    @Test
    @DisplayName("from copia cada campo del resultado")
    void from_copia_cada_campo() {
        PublishAdminPermissionsResult resultado = new PublishAdminPermissionsResult(3, 2, 5, 4);

        PublishAdminPermissionsDto dto = PublishAdminPermissionsDto.from(resultado);

        assertThat(dto.companiesProcessed()).isEqualTo(3);
        assertThat(dto.companiesUpdated()).isEqualTo(2);
        assertThat(dto.permissionsCreated()).isEqualTo(5);
        assertThat(dto.rolePermissionsCreated()).isEqualTo(4);
    }
}
