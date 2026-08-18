package com.vetsoftware.app.systempermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.application.port.out.SystemUserPermissionChildrenQueryPort;
import com.vetsoftware.app.systempermission.domain.SystemPermissionHasActiveChildrenException;
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
@DisplayName("DeleteSystemPermissionService")
class DeleteSystemPermissionServiceTest {

    @Mock
    private SystemPermissionRepository repository;
    @Mock
    private SystemUserPermissionChildrenQueryPort systemUserPermissionChildrenQueryPort;

    private DeleteSystemPermissionService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteSystemPermissionService(repository,
                systemUserPermissionChildrenQueryPort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra el permiso sin usuarios de sistema activos")
        void borra_el_permiso_sin_hijos_activos() {
            when(repository.findById(1L))
                    .thenReturn(Optional.of(SystemPermissionMother.permisoValido()));
            when(systemUserPermissionChildrenQueryPort.existsActiveBySystemPermissionId(1L))
                    .thenReturn(false);

            service.execute(1L);

            verify(repository).delete(1L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no borra si el permiso no existe")
        void no_borra_si_el_permiso_no_existe() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(SystemPermissionNotFoundException.class)
                    .hasMessageContaining("1");

            verifyNoInteractions(systemUserPermissionChildrenQueryPort);
            verify(repository, never()).delete(1L);
        }

        @Test
        @DisplayName("no borra un permiso con usuarios de sistema activos")
        void no_borra_un_permiso_con_hijos_activos() {
            when(repository.findById(1L))
                    .thenReturn(Optional.of(SystemPermissionMother.permisoValido()));
            when(systemUserPermissionChildrenQueryPort.existsActiveBySystemPermissionId(1L))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(SystemPermissionHasActiveChildrenException.class)
                    .hasMessageContaining("1").hasMessageContaining("systemUserPermission");

            verify(repository, never()).delete(1L);
        }
    }
}
