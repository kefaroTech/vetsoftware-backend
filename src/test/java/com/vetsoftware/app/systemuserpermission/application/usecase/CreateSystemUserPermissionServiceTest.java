package com.vetsoftware.app.systemuserpermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemPermissionQueryPort;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserQueryPort;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSystemUserPermissionService")
class CreateSystemUserPermissionServiceTest {

    @Mock
    private SystemUserPermissionRepository repository;
    @Mock
    private SystemUserQueryPort systemUserQueryPort;
    @Mock
    private SystemPermissionQueryPort systemPermissionQueryPort;

    @InjectMocks
    private CreateSystemUserPermissionService service;

    @Captor
    private ArgumentCaptor<SystemUserPermission> captor;

    private void referenciasExisten() {
        when(systemUserQueryPort.findById(SystemUserPermissionMother.USUARIO.id()))
                .thenReturn(Optional.of(SystemUserPermissionMother.USUARIO));
        when(systemPermissionQueryPort.findById(SystemUserPermissionMother.PERMISO.id()))
                .thenReturn(Optional.of(SystemUserPermissionMother.PERMISO));
    }

    @Nested
    @DisplayName("referencias inexistentes")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("systemUser inexistente: no consulta el resto ni persiste")
        void system_user_inexistente() {
            when(systemUserQueryPort.findById(SystemUserPermissionMother.USUARIO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "SystemUser not found: " + SystemUserPermissionMother.USUARIO.id());

            verifyNoInteractions(systemPermissionQueryPort, repository);
        }

        @Test
        @DisplayName("systemPermission inexistente: no persiste")
        void system_permission_inexistente() {
            when(systemUserQueryPort.findById(SystemUserPermissionMother.USUARIO.id()))
                    .thenReturn(Optional.of(SystemUserPermissionMother.USUARIO));
            when(systemPermissionQueryPort.findById(SystemUserPermissionMother.PERMISO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SystemPermission not found: "
                            + SystemUserPermissionMother.PERMISO.id());

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("asignacion deshabilitada previa — reactiva en vez de duplicar")
    class AsignacionDeshabilitadaPrevia {

        @Test
        @DisplayName("reactiva la fila existente y no crea una nueva")
        void reactiva_la_fila_existente_y_no_crea_una_nueva() {
            referenciasExisten();
            when(repository.findDisabledIdBySystemUserAndSystemPermission(
                    SystemUserPermissionMother.USUARIO.id(),
                    SystemUserPermissionMother.PERMISO.id()))
                    .thenReturn(Optional.of(SystemUserPermissionMother.ID));
            when(repository.findById(SystemUserPermissionMother.ID))
                    .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));

            SystemUserPermissionDto dto = service
                    .execute(SystemUserPermissionMother.comandoCrear());

            verify(repository).reactivate(SystemUserPermissionMother.ID);
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.ID);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("si tras reactivar la fila ya no aparece, falla en vez de devolver datos a medias")
        void si_tras_reactivar_la_fila_no_aparece_falla() {
            referenciasExisten();
            when(repository.findDisabledIdBySystemUserAndSystemPermission(
                    SystemUserPermissionMother.USUARIO.id(),
                    SystemUserPermissionMother.PERMISO.id()))
                    .thenReturn(Optional.of(SystemUserPermissionMother.ID));
            when(repository.findById(SystemUserPermissionMother.ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.comandoCrear()))
                    .isInstanceOf(SystemUserPermissionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SystemUserPermissionMother.ID));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("sin asignacion previa")
    class SinAsignacionPrevia {

        @Test
        @DisplayName("crea la asignacion con las referencias resueltas por los puertos")
        void crea_la_asignacion_con_las_referencias_resueltas() {
            referenciasExisten();
            when(repository.findDisabledIdBySystemUserAndSystemPermission(
                    SystemUserPermissionMother.USUARIO.id(),
                    SystemUserPermissionMother.PERMISO.id())).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(SystemUserPermissionMother.asignacionActiva());

            SystemUserPermissionDto dto = service
                    .execute(SystemUserPermissionMother.comandoCrear());

            verify(repository).save(captor.capture());
            SystemUserPermission guardado = captor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.getSystemUser()).isEqualTo(SystemUserPermissionMother.USUARIO);
            assertThat(guardado.getSystemPermission())
                    .isEqualTo(SystemUserPermissionMother.PERMISO);
            assertThat(guardado.isEnabled()).isTrue();
            assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.ID);
        }
    }
}
