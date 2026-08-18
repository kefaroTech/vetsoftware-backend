package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.registration.application.port.out.RoleCreator.RoleResult;
import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.CompanySummaryDto;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRoleAdapter")
class CreateRoleAdapterTest {

    @Mock
    private CreateRoleUseCase createRoleUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @InjectMocks
    private CreateRoleAdapter adapter;

    @BeforeEach
    void setUp() {
        when(systemAuthRunner.call(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private static RoleDto dto() {
        return new RoleDto(100L, "Administrador", "ADMIN",
                new CompanySummaryDto(9L, "Veterinaria Vetrina", "900123456"),
                LocalDateTime.of(2026, 1, 15, 10, 30), List.of(), true);
    }

    @Test
    @DisplayName("crea el rol de la empresa con los datos mapeados")
    void crea_el_rol_con_los_datos_mapeados() {
        when(createRoleUseCase.execute(any(CreateRoleCommand.class))).thenReturn(dto());

        RoleResult result = adapter.create("Administrador", "ADMIN", 9L);

        ArgumentCaptor<CreateRoleCommand> captor = ArgumentCaptor.forClass(CreateRoleCommand.class);
        verify(createRoleUseCase).execute(captor.capture());
        CreateRoleCommand command = captor.getValue();
        assertThat(command.name()).isEqualTo("Administrador");
        assertThat(command.code()).isEqualTo("ADMIN");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(result.id()).isEqualTo(100L);
    }
}
