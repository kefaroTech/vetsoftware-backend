package com.vetsoftware.app.baserolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.baserolepermission.testsupport.BaseRolePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteBaseRolePermissionService")
class DeleteBaseRolePermissionServiceTest {

    @Mock
    private BaseRolePermissionRepository repository;
    @InjectMocks
    private DeleteBaseRolePermissionService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina el vinculo cuando existe")
        void elimina_el_vinculo_cuando_existe() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(BaseRolePermissionMother.vinculo()));

            service.execute(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);

            verify(repository).delete(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BaseRolePermissionNotFoundException si el vinculo no existe y no elimina")
        void lanza_not_found_si_el_vinculo_no_existe() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .isInstanceOf(BaseRolePermissionNotFoundException.class)
                    .hasMessageContaining("BaseRolePermission not found: "
                            + BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);

            verify(repository, never()).delete(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }
    }
}
