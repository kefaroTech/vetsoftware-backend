package com.vetsoftware.app.module.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.module.domain.Module;
import com.vetsoftware.app.module.testsupport.ModuleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModuleDto.from")
class ModuleDtoTest {

    @Test
    @DisplayName("mapea cada campo del modulo, campo por campo")
    void mapea_cada_campo() {
        Module module = ModuleMother.moduloValido();

        ModuleDto dto = ModuleDto.from(module);

        assertThat(dto.id()).isEqualTo(module.getId());
        assertThat(dto.name()).isEqualTo(module.getName());
        assertThat(dto.code()).isEqualTo(module.getCode());
        assertThat(dto.createdDate()).isEqualTo(module.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(module.isEnabled());
    }

    @Test
    @DisplayName("un modulo deshabilitado mapea enabled en false")
    void un_modulo_deshabilitado_mapea_enabled_en_false() {
        Module module = ModuleMother.moduloValido();
        module.disable();

        assertThat(ModuleDto.from(module).enabled()).isFalse();
    }
}
