package com.vetsoftware.app.publishadminpermissions.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdminBasePermission — plantilla de permiso base del rol ADMIN")
class AdminBasePermissionTest {

    @Test
    @DisplayName("expone cada campo con el que fue construido")
    void expone_cada_campo() {
        AdminBasePermission plantilla = new AdminBasePermission(101L, "animal.read", "Ver animales",
                5L);

        assertThat(plantilla.id()).isEqualTo(101L);
        assertThat(plantilla.code()).isEqualTo("animal.read");
        assertThat(plantilla.name()).isEqualTo("Ver animales");
        assertThat(plantilla.subModuleId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("dos plantillas con los mismos campos son iguales")
    void dos_plantillas_con_los_mismos_campos_son_iguales() {
        AdminBasePermission a = new AdminBasePermission(101L, "animal.read", "Ver animales", 5L);
        AdminBasePermission b = new AdminBasePermission(101L, "animal.read", "Ver animales", 5L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
