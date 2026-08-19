package com.vetsoftware.app.role.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RoleDto — from(Role)")
class RoleDtoTest {

    private static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Norte", "NIT-900");
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Role role() {
        return new Role(1L, "Veterinario", "VET", CLINICA, CREADO, null, true);
    }

    @Nested
    @DisplayName("from(Role) sin permisos")
    class SinPermisos {

        @Test
        @DisplayName("copia cada campo y deja los permisos vacios")
        void copia_cada_campo_y_deja_los_permisos_vacios() {
            RoleDto dto = RoleDto.from(role());

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Veterinario");
            assertThat(dto.code()).isEqualTo("VET");
            assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(CLINICA));
            assertThat(dto.createdDate()).isEqualTo(CREADO);
            assertThat(dto.permissions()).isEmpty();
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("from(Role, permissions)")
    class ConPermisos {

        @Test
        @DisplayName("propaga la lista de permisos recibida")
        void propaga_la_lista_de_permisos_recibida() {
            List<PermissionSummaryDto> permisos = List
                    .of(new PermissionSummaryDto(1L, 2L, "Ver animales", "ANIMAL_READ"));

            RoleDto dto = RoleDto.from(role(), permisos);

            assertThat(dto.permissions()).isEqualTo(permisos);
        }

        @Test
        @DisplayName("un rol deshabilitado tambien se refleja en el dto")
        void un_rol_deshabilitado_se_refleja_en_el_dto() {
            Role deshabilitado = new Role(1L, "Veterinario", "VET", CLINICA, CREADO, null, false);

            RoleDto dto = RoleDto.from(deshabilitado, List.of());

            assertThat(dto.enabled()).isFalse();
        }
    }
}
