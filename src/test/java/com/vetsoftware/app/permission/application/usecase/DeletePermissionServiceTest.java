package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.RolePermissionChildrenQueryPort;
import com.vetsoftware.app.permission.domain.PermissionHasActiveChildrenException;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePermissionService")
class DeletePermissionServiceTest {

    @Mock
    private PermissionRepository repository;
    @Mock
    private RolePermissionChildrenQueryPort rolePermissionChildrenQueryPort;

    @InjectMocks
    private DeletePermissionService service;

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("sin hijos activos: borra el permiso")
        void sin_hijos_activos_borra_el_permiso() {
            when(repository.findById(PermissionMother.PERMISSION_ID))
                    .thenReturn(Optional.of(PermissionMother.permisoValido()));
            when(rolePermissionChildrenQueryPort
                    .existsActiveByPermissionId(PermissionMother.PERMISSION_ID)).thenReturn(false);

            service.execute(PermissionMother.PERMISSION_ID);

            verify(repository).delete(PermissionMother.PERMISSION_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("permiso inexistente: no consulta hijos ni borra")
        void permiso_inexistente() {
            when(repository.findById(PermissionMother.PERMISSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PermissionMother.PERMISSION_ID))
                    .isInstanceOf(PermissionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PermissionMother.PERMISSION_ID));

            verifyNoInteractions(rolePermissionChildrenQueryPort);
        }

        @Test
        @DisplayName("con role-permissions activos: rechaza el borrado y no lo ejecuta")
        void con_hijos_activos_rechaza_el_borrado() {
            when(repository.findById(PermissionMother.PERMISSION_ID))
                    .thenReturn(Optional.of(PermissionMother.permisoValido()));
            when(rolePermissionChildrenQueryPort
                    .existsActiveByPermissionId(PermissionMother.PERMISSION_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(PermissionMother.PERMISSION_ID))
                    .isInstanceOf(PermissionHasActiveChildrenException.class)
                    .hasMessageContaining(String.valueOf(PermissionMother.PERMISSION_ID))
                    .hasMessageContaining("rolePermission");

            verify(repository, never()).delete(any());
        }
    }
}
