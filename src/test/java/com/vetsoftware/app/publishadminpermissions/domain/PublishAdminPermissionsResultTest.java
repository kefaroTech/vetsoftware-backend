package com.vetsoftware.app.publishadminpermissions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PublishAdminPermissionsResult — agregado de la publicacion")
class PublishAdminPermissionsResultTest {

    @Test
    @DisplayName("expone cada contador con el que fue construido")
    void expone_cada_contador() {
        PublishAdminPermissionsResult resultado = new PublishAdminPermissionsResult(3, 2, 5, 4);

        assertThat(resultado.companiesProcessed()).isEqualTo(3);
        assertThat(resultado.companiesUpdated()).isEqualTo(2);
        assertThat(resultado.permissionsCreated()).isEqualTo(5);
        assertThat(resultado.rolePermissionsCreated()).isEqualTo(4);
    }
}
