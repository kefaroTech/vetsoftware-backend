package com.vetsoftware.app.basepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.application.port.out.BaseRolePermissionChildrenQueryPort;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.domain.BasePermissionHasActiveChildrenException;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import com.vetsoftware.app.basepermission.testsupport.BasePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteBasePermissionService")
class DeleteBasePermissionServiceTest {

    @Mock
    private BasePermissionRepository repository;
    @Mock
    private BaseRolePermissionChildrenQueryPort baseRolePermissionChildrenQueryPort;
    @InjectMocks
    private DeleteBasePermissionService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina el permiso base cuando no tiene baseRolePermission activos")
        void elimina_el_permiso_cuando_no_tiene_hijos_activos() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.of(BasePermissionMother.crearFactura()));
            when(baseRolePermissionChildrenQueryPort
                    .existsActiveByBasePermissionId(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(false);

            service.execute(BasePermissionMother.BASE_PERMISSION_ID);

            verify(repository).delete(BasePermissionMother.BASE_PERMISSION_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BasePermissionNotFoundException si el permiso no existe y no consulta hijos")
        void lanza_not_found_si_el_permiso_no_existe() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BasePermissionMother.BASE_PERMISSION_ID))
                    .isInstanceOf(BasePermissionNotFoundException.class)
                    .hasMessageContaining("BasePermission not found with id: "
                            + BasePermissionMother.BASE_PERMISSION_ID);

            verifyNoInteractions(baseRolePermissionChildrenQueryPort);
        }

        @Test
        @DisplayName("no elimina si el permiso tiene baseRolePermission activos")
        void no_elimina_si_tiene_hijos_activos() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.of(BasePermissionMother.crearFactura()));
            when(baseRolePermissionChildrenQueryPort
                    .existsActiveByBasePermissionId(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(BasePermissionMother.BASE_PERMISSION_ID))
                    .isInstanceOf(BasePermissionHasActiveChildrenException.class)
                    .hasMessageContaining("baseRolePermission");

            verify(repository, never()).delete(BasePermissionMother.BASE_PERMISSION_ID);
        }
    }
}
