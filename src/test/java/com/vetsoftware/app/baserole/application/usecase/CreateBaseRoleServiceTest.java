package com.vetsoftware.app.baserole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.out.BaseRolePermissionInitializationPort;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBaseRoleService")
class CreateBaseRoleServiceTest {

    @Mock
    private BaseRoleRepository repository;
    @Mock
    private BaseRolePermissionInitializationPort initializationPort;

    @InjectMocks
    private CreateBaseRoleService service;

    @Nested
    @DisplayName("rol obligatorio")
    class RolObligatorio {

        @Test
        @DisplayName("mandatory=true inicializa el rol con todos los permisos base")
        void mandatory_true_inicializa_el_rol_con_todos_los_permisos_base() {
            CreateBaseRoleCommand command = new CreateBaseRoleCommand("Administrador", "ADMIN",
                    true);
            when(repository.save(any())).thenAnswer(inv -> {
                BaseRole created = inv.getArgument(0);
                return new BaseRole(3L, created.getName(), created.getCode(),
                        created.getMandatory(), created.getCreatedDate(), created.getVersion(),
                        created.isEnabled());
            });

            BaseRoleDto result = service.execute(command);

            assertThat(result.id()).isEqualTo(3L);
            assertThat(result.mandatory()).isTrue();
            verify(initializationPort).initializeForAllBasePermissions(3L);
        }
    }

    @Nested
    @DisplayName("rol opcional")
    class RolOpcional {

        @Test
        @DisplayName("mandatory=false no inicializa permisos")
        void mandatory_false_no_inicializa_permisos() {
            CreateBaseRoleCommand command = new CreateBaseRoleCommand("Veterinario", "VET", false);
            when(repository.save(any())).thenAnswer(inv -> {
                BaseRole created = inv.getArgument(0);
                return new BaseRole(1L, created.getName(), created.getCode(),
                        created.getMandatory(), created.getCreatedDate(), created.getVersion(),
                        created.isEnabled());
            });

            BaseRoleDto result = service.execute(command);

            assertThat(result.mandatory()).isFalse();
            verifyNoInteractions(initializationPort);
        }
    }

    @Test
    @DisplayName("persiste el rol con los datos del command")
    void persiste_el_rol_con_los_datos_del_command() {
        CreateBaseRoleCommand command = new CreateBaseRoleCommand("Veterinario", "VET", false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(command);

        ArgumentCaptor<BaseRole> captor = ArgumentCaptor.forClass(BaseRole.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Veterinario");
        assertThat(captor.getValue().getCode()).isEqualTo("VET");
        assertThat(captor.getValue().getId()).isNull();
    }
}
