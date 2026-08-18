package com.vetsoftware.app.baserolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
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
@DisplayName("FindBaseRolePermissionService")
class FindBaseRolePermissionServiceTest {

    @Mock
    private BaseRolePermissionRepository repository;
    @InjectMocks
    private FindBaseRolePermissionService service;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el dto del vinculo encontrado")
        void devuelve_el_dto_del_vinculo_encontrado() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(BaseRolePermissionMother.vinculo()));

            BaseRolePermissionDto dto = service
                    .findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);

            assertThat(dto.id()).isEqualTo(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
            assertThat(dto.baseRole().code()).isEqualTo("VET");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BaseRolePermissionNotFoundException si no existe")
        void lanza_not_found_si_no_existe() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .isInstanceOf(BaseRolePermissionNotFoundException.class)
                    .hasMessageContaining("BaseRolePermission not found: "
                            + BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }
    }
}
