package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import com.vetsoftware.app.role.testsupport.RoleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindRoleService")
class FindRoleServiceTest {

    @Mock
    private RoleRepository repository;

    private FindRoleService service;

    @BeforeEach
    void crearServicio() {
        service = new FindRoleService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el rol encontrado mapeado a dto")
        void devuelve_el_rol_encontrado_mapeado_a_dto() {
            when(repository.findById(RoleMother.ROLE_ID))
                    .thenReturn(Optional.of(RoleMother.veterinario()));

            RoleDto dto = service.findById(RoleMother.ROLE_ID);

            assertThat(dto.id()).isEqualTo(RoleMother.ROLE_ID);
            assertThat(dto.name()).isEqualTo("Veterinario");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("un id inexistente lanza RoleNotFoundException")
        void un_id_inexistente_lanza_role_not_found() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(RoleNotFoundException.class).hasMessageContaining("99");
        }
    }
}
