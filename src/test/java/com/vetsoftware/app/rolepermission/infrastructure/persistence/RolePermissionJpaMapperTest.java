package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link RoleJpaEntity} pertenece a otra feature y su constructor es
 * {@code protected}: desde este paquete no se puede instanciar, asi que se
 * mockea. No es una entidad de dominio —no tiene invariantes— sino una fila,
 * por lo que el doble no oculta ninguna regla de negocio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RolePermissionJpaMapper — ida y vuelta dominio↔entidad")
class RolePermissionJpaMapperTest {

    private final RolePermissionJpaMapper mapper = new RolePermissionJpaMapper();

    @Mock
    private RoleJpaEntity roleEntity;

    private static PermissionJpaEntity permissionEntity() {
        PermissionJpaEntity permission = new PermissionJpaEntity();
        permission.setId(7L);
        permission.setName("Ver animales");
        permission.setCode("ANIMAL_READ");
        return permission;
    }

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia id, fecha y estado, y engancha las dos relaciones recibidas")
        void copia_los_campos_y_las_relaciones() {
            PermissionJpaEntity permission = permissionEntity();

            RolePermissionJpaEntity entity = mapper.toJpa(RolePermissionMother.activa(), roleEntity,
                    permission);

            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getRole()).isSameAs(roleEntity);
            assertThat(entity.getPermission()).isSameAs(permission);
            assertThat(entity.getCreatedDate()).isEqualTo(RolePermissionMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una asignacion nueva viaja sin id para que lo genere la base")
        void una_asignacion_nueva_viaja_sin_id() {
            RolePermission nueva = RolePermission.create(RolePermissionMother.VETERINARIO,
                    RolePermissionMother.VER_ANIMALES);

            RolePermissionJpaEntity entity = mapper.toJpa(nueva, roleEntity, permissionEntity());

            assertThat(entity.getId()).isNull();
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("propaga el estado deshabilitado")
        void propaga_el_estado_deshabilitado() {
            RolePermissionJpaEntity entity = mapper.toJpa(RolePermissionMother.desactivada(),
                    roleEntity, permissionEntity());

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("construye las referencias leyendo las entidades relacionadas")
        void construye_las_referencias_leyendo_las_relaciones() {
            when(roleEntity.getId()).thenReturn(3L);
            when(roleEntity.getName()).thenReturn("Veterinario");
            when(roleEntity.getCode()).thenReturn("VET");
            RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
            entity.setId(1L);
            entity.setRole(roleEntity);
            entity.setPermission(permissionEntity());
            entity.setCreatedDate(RolePermissionMother.CREADO);
            entity.setEnabled(true);

            RolePermission domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(1L);
            assertThat(domain.getRole()).isEqualTo(new RoleRef(3L, "Veterinario", "VET"));
            assertThat(domain.getPermission())
                    .isEqualTo(new PermissionRef(7L, "Ver animales", "ANIMAL_READ"));
            assertThat(domain.getCreatedDate()).isEqualTo(RolePermissionMother.CREADO);
            assertThat(domain.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la sobrecarga con refs no toca las relaciones de la entidad")
        void la_sobrecarga_con_refs_no_toca_las_relaciones() {
            RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
            entity.setId(1L);
            entity.setCreatedDate(RolePermissionMother.CREADO);
            entity.setEnabled(false);

            RolePermission domain = mapper.toDomain(entity, RolePermissionMother.RECEPCION,
                    RolePermissionMother.CREAR_ANIMALES);

            assertThat(domain.getRole()).isEqualTo(RolePermissionMother.RECEPCION);
            assertThat(domain.getPermission()).isEqualTo(RolePermissionMother.CREAR_ANIMALES);
            assertThat(domain.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio → entidad → dominio conserva todos los campos")
        void conserva_todos_los_campos() {
            RolePermission original = RolePermissionMother.activa();

            RolePermissionJpaEntity entity = mapper.toJpa(original, roleEntity, permissionEntity());
            RolePermission vuelta = mapper.toDomain(entity, original.getRole(),
                    original.getPermission());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getRole()).isEqualTo(original.getRole());
            assertThat(vuelta.getPermission()).isEqualTo(original.getPermission());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
