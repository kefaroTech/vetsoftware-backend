package com.vetsoftware.app.publishadminpermissions.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpsertedPermission — resultado del upsert de un permiso")
class UpsertedPermissionTest {

    @Test
    @DisplayName("expone el id y si fue creado")
    void expone_el_id_y_si_fue_creado() {
        UpsertedPermission creado = new UpsertedPermission(77L, true);
        UpsertedPermission existente = new UpsertedPermission(900L, false);

        assertThat(creado.id()).isEqualTo(77L);
        assertThat(creado.created()).isTrue();
        assertThat(existente.id()).isEqualTo(900L);
        assertThat(existente.created()).isFalse();
    }
}
