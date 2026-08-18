package com.vetsoftware.app.baserolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionQueryPort;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleQueryPort;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.baserolepermission.testsupport.BaseRolePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateBaseRolePermissionService")
class UpdateBaseRolePermissionServiceTest {

    @Mock
    private BaseRolePermissionRepository repository;
    @Mock
    private BaseRoleQueryPort baseRoleQueryPort;
    @Mock
    private BasePermissionQueryPort basePermissionQueryPort;
    @InjectMocks
    private UpdateBaseRolePermissionService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza rol y permiso y persiste el agregado")
        void actualiza_rol_y_permiso_y_persiste_el_agregado() {
            BaseRolePermission existente = BaseRolePermissionMother.vinculo();
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(existente));
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.ADMINISTRADOR.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.ADMINISTRADOR));
            when(basePermissionQueryPort.findById(BaseRolePermissionMother.EDITAR_CONSULTA.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.EDITAR_CONSULTA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BaseRolePermissionDto dto = service
                    .execute(BaseRolePermissionMother.comandoActualizar());

            ArgumentCaptor<BaseRolePermission> captor = ArgumentCaptor
                    .forClass(BaseRolePermission.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getBaseRole())
                    .isEqualTo(BaseRolePermissionMother.ADMINISTRADOR);
            assertThat(captor.getValue().getBasePermission())
                    .isEqualTo(BaseRolePermissionMother.EDITAR_CONSULTA);
            assertThat(dto.baseRole().code()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BaseRolePermissionNotFoundException si el vinculo no existe y no consulta los puertos")
        void lanza_not_found_si_el_vinculo_no_existe() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BaseRolePermissionMother.comandoActualizar()))
                    .isInstanceOf(BaseRolePermissionNotFoundException.class)
                    .hasMessageContaining("BaseRolePermission not found: "
                            + BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);

            verifyNoInteractions(baseRoleQueryPort);
            verifyNoInteractions(basePermissionQueryPort);
        }

        @Test
        @DisplayName("no guarda si el rol destino no existe")
        void no_guarda_si_el_rol_destino_no_existe() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(BaseRolePermissionMother.vinculo()));
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.ADMINISTRADOR.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BaseRolePermissionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "BaseRole not found: " + BaseRolePermissionMother.ADMINISTRADOR.id());

            verifyNoInteractions(basePermissionQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda si el permiso destino no existe")
        void no_guarda_si_el_permiso_destino_no_existe() {
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(BaseRolePermissionMother.vinculo()));
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.ADMINISTRADOR.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.ADMINISTRADOR));
            when(basePermissionQueryPort.findById(BaseRolePermissionMother.EDITAR_CONSULTA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BaseRolePermissionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BasePermission not found: "
                            + BaseRolePermissionMother.EDITAR_CONSULTA.id());

            verify(repository, never()).save(any());
        }
    }
}
