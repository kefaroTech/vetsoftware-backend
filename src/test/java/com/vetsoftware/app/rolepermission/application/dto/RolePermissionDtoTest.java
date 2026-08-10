package com.vetsoftware.app.rolepermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de rolepermission — from(...) campo por campo")
class RolePermissionDtoTest {

    @Nested
    @DisplayName("RolePermissionDto")
    class RolePermissionDtoMapeo {

        @Test
        @DisplayName("copia id, fecha, estado y las dos referencias anidadas")
        void copia_todos_los_campos() {
            RolePermissionDto dto = RolePermissionDto.from(RolePermissionMother.activa());

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.createdDate()).isEqualTo(RolePermissionMother.CREADO);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.role()).isEqualTo(new RoleSummaryDto(3L, "Veterinario", "VET"));
            assertThat(dto.permission())
                    .isEqualTo(new PermissionSummaryDto(7L, "Ver animales", "ANIMAL_READ"));
        }

        @Test
        @DisplayName("refleja el estado deshabilitado de la asignacion")
        void refleja_el_estado_deshabilitado() {
            assertThat(RolePermissionDto.from(RolePermissionMother.desactivada()).enabled())
                    .isFalse();
        }

        @Test
        @DisplayName("una asignacion recien creada sale sin id")
        void una_asignacion_recien_creada_sale_sin_id() {
            RolePermission recien = RolePermission.create(RolePermissionMother.RECEPCION,
                    RolePermissionMother.CREAR_ANIMALES);

            RolePermissionDto dto = RolePermissionDto.from(recien);

            assertThat(dto.id()).isNull();
            assertThat(dto.role().code()).isEqualTo("REC");
            assertThat(dto.permission().code()).isEqualTo("ANIMAL_CREATE");
        }
    }

    @Nested
    @DisplayName("RoleSummaryDto")
    class RoleSummary {

        @Test
        @DisplayName("copia id, nombre y codigo del RoleRef")
        void copia_id_nombre_y_codigo() {
            RoleSummaryDto dto = RoleSummaryDto.from(RolePermissionMother.VETERINARIO);

            assertThat(dto.id()).isEqualTo(3L);
            assertThat(dto.name()).isEqualTo("Veterinario");
            assertThat(dto.code()).isEqualTo("VET");
        }
    }

    @Nested
    @DisplayName("PermissionSummaryDto")
    class PermissionSummary {

        @Test
        @DisplayName("copia id, nombre y codigo del PermissionRef")
        void copia_id_nombre_y_codigo() {
            PermissionSummaryDto dto = PermissionSummaryDto
                    .from(RolePermissionMother.CREAR_ANIMALES);

            assertThat(dto.id()).isEqualTo(8L);
            assertThat(dto.name()).isEqualTo("Crear animales");
            assertThat(dto.code()).isEqualTo("ANIMAL_CREATE");
        }
    }
}
