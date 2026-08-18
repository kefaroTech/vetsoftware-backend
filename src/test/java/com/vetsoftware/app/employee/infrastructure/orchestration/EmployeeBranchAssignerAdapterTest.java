package com.vetsoftware.app.employee.infrastructure.orchestration;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Adaptador de orquestación: delega en la feature {@code employeebranch}. */
@ExtendWith(MockitoExtension.class)
class EmployeeBranchAssignerAdapterTest {

    @Mock
    private SetEmployeeBranchesUseCase setUseCase;
    @InjectMocks
    private EmployeeBranchAssignerAdapter adapter;

    @Test
    @DisplayName("arma el set atomico de sedes con replaceAll en false para no borrar asignaciones previas")
    void arma_el_set_atomico_de_sedes() {
        adapter.assign(55L, 9L, List.of(7L, 8L));

        verify(setUseCase).execute(new SetEmployeeBranchesCommand(55L, 9L, false, List.of(7L, 8L)));
    }
}
