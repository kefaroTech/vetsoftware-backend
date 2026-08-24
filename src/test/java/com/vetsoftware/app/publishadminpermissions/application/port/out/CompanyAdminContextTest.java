package com.vetsoftware.app.publishadminpermissions.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyAdminContext — empresa con su rol ADMIN resuelto")
class CompanyAdminContextTest {

    @Test
    @DisplayName("expone cada campo con el que fue construido")
    void expone_cada_campo() {
        CompanyAdminContext contexto = new CompanyAdminContext(1L, 200L);

        assertThat(contexto.companyId()).isEqualTo(1L);
        assertThat(contexto.adminRoleId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("dos contextos con los mismos campos son iguales")
    void dos_contextos_con_los_mismos_campos_son_iguales() {
        CompanyAdminContext a = new CompanyAdminContext(1L, 200L);
        CompanyAdminContext b = new CompanyAdminContext(1L, 200L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
