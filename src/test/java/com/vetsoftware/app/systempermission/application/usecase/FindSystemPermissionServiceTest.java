package com.vetsoftware.app.systempermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import com.vetsoftware.app.systempermission.testsupport.SystemPermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSystemPermissionService")
class FindSystemPermissionServiceTest {

    @Mock
    private SystemPermissionRepository repository;

    private FindSystemPermissionService service;

    @BeforeEach
    void crearServicio() {
        service = new FindSystemPermissionService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el permiso encontrado mapeado a dto")
        void devuelve_el_permiso_encontrado() {
            when(repository.findById(1L))
                    .thenReturn(Optional.of(SystemPermissionMother.permisoValido()));

            SystemPermissionDto dto = service.findById(1L);

            assertThat(dto.id()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("un permiso inexistente lanza SystemPermissionNotFoundException")
        void permiso_inexistente_lanza_excepcion() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(SystemPermissionNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }
}
