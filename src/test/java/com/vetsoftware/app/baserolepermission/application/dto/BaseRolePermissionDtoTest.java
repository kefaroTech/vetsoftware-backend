package com.vetsoftware.app.baserolepermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRolePermissionDto — mapeo desde el dominio")
class BaseRolePermissionDtoTest {

    @Test
    @DisplayName("from() copia cada campo, incluido el rol y el permiso resumidos")
    void from_copia_cada_campo() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BaseRolePermission vinculo = new BaseRolePermission(2L,
                new BaseRoleRef(1L, "Veterinario", "VET"),
                new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"), creado, true);

        BaseRolePermissionDto dto = BaseRolePermissionDto.from(vinculo);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.baseRole()).isEqualTo(new BaseRoleSummaryDto(1L, "Veterinario", "VET"));
        assertThat(dto.basePermission())
                .isEqualTo(new BasePermissionSummaryDto(10L, "Crear consulta", "CONSULTA_CREATE"));
        assertThat(dto.createdDate()).isEqualTo(creado);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() conserva enabled=false de un vinculo deshabilitado")
    void from_conserva_enabled_false_de_un_vinculo_deshabilitado() {
        BaseRolePermission vinculo = new BaseRolePermission(2L,
                new BaseRoleRef(1L, "Veterinario", "VET"),
                new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"),
                LocalDateTime.of(2026, 1, 15, 10, 30), false);

        assertThat(BaseRolePermissionDto.from(vinculo).enabled()).isFalse();
    }
}
