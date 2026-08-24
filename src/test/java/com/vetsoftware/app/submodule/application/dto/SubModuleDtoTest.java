package com.vetsoftware.app.submodule.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.submodule.domain.ModuleRef;
import com.vetsoftware.app.submodule.domain.SubModule;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubModuleDto — mapeo desde el dominio")
class SubModuleDtoTest {

    @Test
    @DisplayName("from() copia cada campo, incluido el modulo resumido")
    void from_copia_cada_campo() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        SubModule subModule = new SubModule(2L, "Reportes", "REP",
                new ModuleRef(1L, "Facturacion", "FACT"), true, false, creado, null, true);

        SubModuleDto dto = SubModuleDto.from(subModule);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Reportes");
        assertThat(dto.code()).isEqualTo("REP");
        assertThat(dto.module()).isEqualTo(new ModuleSummaryDto(1L, "Facturacion", "FACT"));
        assertThat(dto.sellable()).isTrue();
        assertThat(dto.readOnlyCapable()).isFalse();
        assertThat(dto.createdDate()).isEqualTo(creado);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() propaga las dos banderas en falso de un submodulo interno")
    void from_propaga_las_dos_banderas_en_falso() {
        SubModule interno = new SubModule(3L, "Configuracion del sistema", "SYS_CONFIG",
                new ModuleRef(1L, "Facturacion", "FACT"), false, false,
                LocalDateTime.of(2026, 1, 15, 10, 30), null, true);

        SubModuleDto dto = SubModuleDto.from(interno);

        assertThat(dto.sellable()).isFalse();
        assertThat(dto.readOnlyCapable()).isFalse();
    }

    @Test
    @DisplayName("from() conserva enabled=false de un submodulo deshabilitado")
    void from_conserva_enabled_false_de_un_submodulo_deshabilitado() {
        SubModule subModule = new SubModule(2L, "Reportes", "REP",
                new ModuleRef(1L, "Facturacion", "FACT"), true, true,
                LocalDateTime.of(2026, 1, 15, 10, 30), null, false);

        assertThat(SubModuleDto.from(subModule).enabled()).isFalse();
    }
}
