package com.vetsoftware.app.baserolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateBaseRolePermissionService")
class ReactivateBaseRolePermissionServiceTest {

    @Mock
    private BaseRolePermissionRepository repository;
    @InjectMocks
    private ReactivateBaseRolePermissionService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el vinculo releido")
        void reactiva_y_devuelve_el_vinculo_releido() {
            when(repository.reactivate(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(1);
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(BaseRolePermissionMother.vinculo()));

            BaseRolePermissionDto dto = service
                    .execute(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);

            assertThat(dto.id()).isEqualTo(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BaseRolePermissionNotFoundException si no reactivo ninguna fila y no relee")
        void lanza_not_found_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .isInstanceOf(BaseRolePermissionNotFoundException.class)
                    .hasMessageContaining("BaseRolePermission not found: "
                            + BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);

            verify(repository, never()).findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }

        @Test
        @DisplayName("lanza BaseRolePermissionNotFoundException si reactivo la fila pero desaparecio antes de la relectura")
        void lanza_not_found_si_desaparece_entre_reactivar_y_releer() {
            when(repository.reactivate(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(1);
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .isInstanceOf(BaseRolePermissionNotFoundException.class)
                    .hasMessageContaining("BaseRolePermission not found: "
                            + BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }
    }
}
