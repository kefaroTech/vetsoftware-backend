package com.vetsoftware.app.systemuser.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuser.application.port.out.SystemUserPermissionChildrenQueryPort;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUserHasActiveChildrenException;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSystemUserService")
class DeleteSystemUserServiceTest {

    @Mock
    private SystemUserRepository repository;
    @Mock
    private SystemUserPermissionChildrenQueryPort systemUserPermissionChildrenQueryPort;

    @InjectMocks
    private DeleteSystemUserService service;

    private void usuarioExiste() {
        when(repository.findById(SystemUserMother.SYSTEM_USER_ID))
                .thenReturn(Optional.of(SystemUserMother.activo()));
    }

    private void borrar() {
        service.execute(SystemUserMother.SYSTEM_USER_ID);
    }

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin permisos de sistema activos, borra")
        void sin_permisos_activos_borra() {
            usuarioExiste();
            when(systemUserPermissionChildrenQueryPort
                    .existsActiveBySystemUserId(SystemUserMother.SYSTEM_USER_ID)).thenReturn(false);

            borrar();

            verify(repository).delete(SystemUserMother.SYSTEM_USER_ID);
        }
    }

    @Nested
    @DisplayName("usuario inexistente")
    class UsuarioInexistente {

        @Test
        @DisplayName("un usuario inexistente no existe y no consulta permisos hijos")
        void un_usuario_inexistente_no_existe() {
            when(repository.findById(SystemUserMother.SYSTEM_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(DeleteSystemUserServiceTest.this::borrar)
                    .isInstanceOf(SystemUserNotFoundException.class).hasMessageContaining(
                            "SystemUser not found: " + SystemUserMother.SYSTEM_USER_ID);

            verifyNoInteractions(systemUserPermissionChildrenQueryPort);
            verify(repository, never()).delete(SystemUserMother.SYSTEM_USER_ID);
        }
    }

    @Nested
    @DisplayName("bloqueo por permisos de sistema activos")
    class BloqueoPorPermisosActivos {

        @Test
        @DisplayName("con permisos de sistema activos, bloquea y no borra")
        void con_permisos_activos_bloquea_y_no_borra() {
            usuarioExiste();
            when(systemUserPermissionChildrenQueryPort
                    .existsActiveBySystemUserId(SystemUserMother.SYSTEM_USER_ID)).thenReturn(true);

            assertThatThrownBy(DeleteSystemUserServiceTest.this::borrar)
                    .isInstanceOf(SystemUserHasActiveChildrenException.class)
                    .hasMessageContaining(
                            "Cannot delete systemuser " + SystemUserMother.SYSTEM_USER_ID)
                    .hasMessageContaining("has active systemUserPermission children");

            verify(repository, never()).delete(SystemUserMother.SYSTEM_USER_ID);
        }
    }
}
