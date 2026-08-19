package com.vetsoftware.app.basepermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BasePermissionDto — mapeo desde el dominio")
class BasePermissionDtoTest {

    @Test
    @DisplayName("from() copia cada campo, incluido el submodulo resumido")
    void from_copia_cada_campo() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BasePermission basePermission = new BasePermission(2L, "Crear factura", "INVOICE_CREATE",
                new SubModuleRef(1L, "Ventas", "VEN"), creado, null, true);

        BasePermissionDto dto = BasePermissionDto.from(basePermission);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Crear factura");
        assertThat(dto.code()).isEqualTo("INVOICE_CREATE");
        assertThat(dto.subModule()).isEqualTo(new SubModuleSummaryDto(1L, "Ventas", "VEN"));
        assertThat(dto.createdDate()).isEqualTo(creado);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() conserva enabled=false de un permiso base deshabilitado")
    void from_conserva_enabled_false_de_un_permiso_deshabilitado() {
        BasePermission basePermission = new BasePermission(2L, "Crear factura", "INVOICE_CREATE",
                new SubModuleRef(1L, "Ventas", "VEN"), LocalDateTime.of(2026, 1, 15, 10, 30), null,
                false);

        assertThat(BasePermissionDto.from(basePermission).enabled()).isFalse();
    }
}
