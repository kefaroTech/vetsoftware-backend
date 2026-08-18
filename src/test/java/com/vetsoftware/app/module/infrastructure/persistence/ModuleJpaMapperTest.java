package com.vetsoftware.app.module.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.module.domain.Module;
import com.vetsoftware.app.module.testsupport.ModuleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModuleJpaMapper — ida y vuelta dominio <-> entidad")
class ModuleJpaMapperTest {

    private final ModuleJpaMapper mapper = new ModuleJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad")
        void copia_cada_campo_a_la_entidad() {
            Module module = ModuleMother.moduloValido();

            ModuleJpaEntity entity = mapper.toJpa(module);

            assertThat(entity.getId()).isEqualTo(module.getId());
            assertThat(entity.getName()).isEqualTo(module.getName());
            assertThat(entity.getCode()).isEqualTo(module.getCode());
            assertThat(entity.getCreatedDate()).isEqualTo(module.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un modulo deshabilitado se mapea con enabled en false")
        void un_modulo_deshabilitado_mapea_enabled_en_false() {
            Module module = ModuleMother.moduloValido();
            module.disable();

            assertThat(mapper.toJpa(module).isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("la ida y vuelta conserva cada campo")
        void la_ida_y_vuelta_conserva_cada_campo() {
            Module original = ModuleMother.moduloValido();

            Module reconstruido = mapper.toDomain(mapper.toJpa(original));

            assertThat(reconstruido.getId()).isEqualTo(original.getId());
            assertThat(reconstruido.getName()).isEqualTo(original.getName());
            assertThat(reconstruido.getCode()).isEqualTo(original.getCode());
            assertThat(reconstruido.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(reconstruido.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
