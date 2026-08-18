package com.vetsoftware.app.systempermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.application.command.CreateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSystemPermissionService")
class CreateSystemPermissionServiceTest {

    @Mock
    private SystemPermissionRepository repository;

    private CreateSystemPermissionService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateSystemPermissionService(repository);
    }

    @Test
    @DisplayName("persiste el permiso creado con name y code del comando")
    void persiste_el_permiso_creado() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemPermissionDto dto = service
                .execute(new CreateSystemPermissionCommand("Administrar usuarios", "admin.users"));

        ArgumentCaptor<SystemPermission> guardado = ArgumentCaptor.forClass(SystemPermission.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getName()).isEqualTo("Administrar usuarios");
        assertThat(guardado.getValue().getCode()).isEqualTo("admin.users");
        assertThat(dto.name()).isEqualTo("Administrar usuarios");
    }
}
