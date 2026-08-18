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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSystemUserPermissionService")
class UpdateSystemUserPermissionServiceTest {

    @Mock
    private SystemUserPermissionRepository repository;
    @Mock
    private SystemUserQueryPort systemUserQueryPort;
    @Mock
    private SystemPermissionQueryPort systemPermissionQueryPort;

    @InjectMocks
    private UpdateSystemUserPermissionService service;

    @Captor
    private ArgumentCaptor<SystemUserPermission> captor;

    @Test
    @DisplayName("asignacion inexistente: no consulta los puertos de referencia")
    void asignacion_inexistente() {
        when(repository.findById(SystemUserPermissionMother.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.comandoActualizar()))
                .isInstanceOf(SystemUserPermissionNotFoundException.class)
                .hasMessageContaining(String.valueOf(SystemUserPermissionMother.ID));

        verifyNoInteractions(systemUserQueryPort, systemPermissionQueryPort);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("systemUser inexistente: no persiste")
    void system_user_inexistente() {
        when(repository.findById(SystemUserPermissionMother.ID))
                .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));
        when(systemUserQueryPort.findById(SystemUserPermissionMother.OTRO_USUARIO.id()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.comandoActualizar()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                        "SystemUser not found: " + SystemUserPermissionMother.OTRO_USUARIO.id());

        verifyNoInteractions(systemPermissionQueryPort);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("systemPermission inexistente: no persiste")
    void system_permission_inexistente() {
        when(repository.findById(SystemUserPermissionMother.ID))
                .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));
        when(systemUserQueryPort.findById(SystemUserPermissionMother.OTRO_USUARIO.id()))
                .thenReturn(Optional.of(SystemUserPermissionMother.OTRO_USUARIO));
        when(systemPermissionQueryPort.findById(SystemUserPermissionMother.OTRO_PERMISO.id()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.comandoActualizar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SystemPermission not found: "
                        + SystemUserPermissionMother.OTRO_PERMISO.id());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("reemplaza usuario y permiso y persiste el resultado")
    void reemplaza_usuario_y_permiso_y_persiste() {
        when(repository.findById(SystemUserPermissionMother.ID))
                .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));
        when(systemUserQueryPort.findById(SystemUserPermissionMother.OTRO_USUARIO.id()))
                .thenReturn(Optional.of(SystemUserPermissionMother.OTRO_USUARIO));
        when(systemPermissionQueryPort.findById(SystemUserPermissionMother.OTRO_PERMISO.id()))
                .thenReturn(Optional.of(SystemUserPermissionMother.OTRO_PERMISO));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemUserPermissionDto dto = service
                .execute(SystemUserPermissionMother.comandoActualizar());

        verify(repository).save(captor.capture());
        SystemUserPermission guardado = captor.getValue();
        assertThat(guardado.getSystemUser()).isEqualTo(SystemUserPermissionMother.OTRO_USUARIO);
        assertThat(guardado.getSystemPermission())
                .isEqualTo(SystemUserPermissionMother.OTRO_PERMISO);
        assertThat(dto.systemUser().code())
                .isEqualTo(SystemUserPermissionMother.OTRO_USUARIO.code());
    }
}
