package com.vetsoftware.app.systempermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.application.command.UpdateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermission;
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
@DisplayName("UpdateSystemPermissionService")
class UpdateSystemPermissionServiceTest {

    @Mock
    private SystemPermissionRepository repository;

    private UpdateSystemPermissionService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateSystemPermissionService(repository);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el permiso existente")
        void actualiza_el_permiso_existente() {
            SystemPermission existente = SystemPermissionMother.permisoValido();
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SystemPermissionDto dto = service.execute(
                    new UpdateSystemPermissionCommand(1L, "Administrar roles", "admin.roles"));

            assertThat(dto.name()).isEqualTo("Administrar roles");
            assertThat(dto.code()).isEqualTo("admin.roles");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no guarda si el permiso no existe")
        void no_guarda_si_el_permiso_no_existe() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    new UpdateSystemPermissionCommand(1L, "Administrar roles", "admin.roles")))
                    .isInstanceOf(SystemPermissionNotFoundException.class)
                    .hasMessageContaining("1");

            verify(repository, never()).save(any());
        }
    }
}
