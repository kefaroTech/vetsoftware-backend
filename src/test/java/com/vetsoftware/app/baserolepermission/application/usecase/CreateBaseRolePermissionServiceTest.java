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
@DisplayName("CreateBaseRolePermissionService")
class CreateBaseRolePermissionServiceTest {

    @Mock
    private BaseRolePermissionRepository repository;
    @Mock
    private BaseRoleQueryPort baseRoleQueryPort;
    @Mock
    private BasePermissionQueryPort basePermissionQueryPort;
    @InjectMocks
    private CreateBaseRolePermissionService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el vinculo con el rol y el permiso resueltos por los puertos")
        void persiste_el_vinculo_con_el_rol_y_el_permiso_resueltos() {
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.VETERINARIO.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.VETERINARIO));
            when(basePermissionQueryPort.findById(BaseRolePermissionMother.CREAR_CONSULTA.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.CREAR_CONSULTA));
            when(repository.findDisabledIdByBaseRoleAndBasePermission(
                    BaseRolePermissionMother.VETERINARIO.id(),
                    BaseRolePermissionMother.CREAR_CONSULTA.id())).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BaseRolePermissionDto dto = service.execute(BaseRolePermissionMother.comandoCrear());

            ArgumentCaptor<BaseRolePermission> captor = ArgumentCaptor
                    .forClass(BaseRolePermission.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getBaseRole())
                    .isEqualTo(BaseRolePermissionMother.VETERINARIO);
            assertThat(captor.getValue().getBasePermission())
                    .isEqualTo(BaseRolePermissionMother.CREAR_CONSULTA);
            assertThat(dto.baseRole().code()).isEqualTo("VET");
        }

        @Test
        @DisplayName("reactiva el vinculo deshabilitado existente en vez de crear uno nuevo")
        void reactiva_el_vinculo_deshabilitado_en_vez_de_crear_uno_nuevo() {
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.VETERINARIO.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.VETERINARIO));
            when(basePermissionQueryPort.findById(BaseRolePermissionMother.CREAR_CONSULTA.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.CREAR_CONSULTA));
            when(repository.findDisabledIdByBaseRoleAndBasePermission(
                    BaseRolePermissionMother.VETERINARIO.id(),
                    BaseRolePermissionMother.CREAR_CONSULTA.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID));
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.of(BaseRolePermissionMother.vinculo()));

            BaseRolePermissionDto dto = service.execute(BaseRolePermissionMother.comandoCrear());

            verify(repository).reactivate(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio ni el puerto de permisos si el rol no existe")
        void no_toca_el_repositorio_si_el_rol_no_existe() {
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.VETERINARIO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BaseRolePermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "BaseRole not found: " + BaseRolePermissionMother.VETERINARIO.id());

            verifyNoInteractions(repository);
            verifyNoInteractions(basePermissionQueryPort);
        }

        @Test
        @DisplayName("no guarda si el permiso no existe")
        void no_guarda_si_el_permiso_no_existe() {
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.VETERINARIO.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.VETERINARIO));
            when(basePermissionQueryPort.findById(BaseRolePermissionMother.CREAR_CONSULTA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BaseRolePermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BasePermission not found: "
                            + BaseRolePermissionMother.CREAR_CONSULTA.id());

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("lanza BaseRolePermissionNotFoundException si el vinculo reactivado desaparece antes de la relectura")
        void lanza_not_found_si_desaparece_entre_reactivar_y_releer() {
            when(baseRoleQueryPort.findById(BaseRolePermissionMother.VETERINARIO.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.VETERINARIO));
            when(basePermissionQueryPort.findById(BaseRolePermissionMother.CREAR_CONSULTA.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.CREAR_CONSULTA));
            when(repository.findDisabledIdByBaseRoleAndBasePermission(
                    BaseRolePermissionMother.VETERINARIO.id(),
                    BaseRolePermissionMother.CREAR_CONSULTA.id()))
                    .thenReturn(Optional.of(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID));
            when(repository.findById(BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BaseRolePermissionMother.comandoCrear()))
                    .isInstanceOf(BaseRolePermissionNotFoundException.class)
                    .hasMessageContaining("BaseRolePermission not found: "
                            + BaseRolePermissionMother.BASE_ROLE_PERMISSION_ID);
        }
    }
}
