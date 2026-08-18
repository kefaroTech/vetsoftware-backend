package com.vetsoftware.app.systempermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import com.vetsoftware.app.systempermission.testsupport.SystemPermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SystemPermissionJpaMapper — ida y vuelta dominio <-> entidad")
class SystemPermissionJpaMapperTest {

    private final SystemPermissionJpaMapper mapper = new SystemPermissionJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad")
        void copia_cada_campo_a_la_entidad() {
            SystemPermission permission = SystemPermissionMother.permisoValido();

            SystemPermissionJpaEntity entity = mapper.toJpa(permission);

            assertThat(entity.getId()).isEqualTo(permission.getId());
            assertThat(entity.getName()).isEqualTo(permission.getName());
            assertThat(entity.getCode()).isEqualTo(permission.getCode());
            assertThat(entity.getCreatedDate()).isEqualTo(permission.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un permiso deshabilitado se mapea con enabled en false")
        void un_permiso_deshabilitado_mapea_enabled_en_false() {
            SystemPermission permission = SystemPermissionMother.permisoValido();
            permission.disable();

            assertThat(mapper.toJpa(permission).isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("la ida y vuelta conserva cada campo")
        void la_ida_y_vuelta_conserva_cada_campo() {
            SystemPermission original = SystemPermissionMother.permisoValido();

            SystemPermission reconstruido = mapper.toDomain(mapper.toJpa(original));

            assertThat(reconstruido.getId()).isEqualTo(original.getId());
            assertThat(reconstruido.getName()).isEqualTo(original.getName());
            assertThat(reconstruido.getCode()).isEqualTo(original.getCode());
            assertThat(reconstruido.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(reconstruido.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
