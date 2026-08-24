package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.in.GetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetEmployeeBranchesAdapter — la atadura del dueño con su sede (#510)")
class SetEmployeeBranchesAdapterTest {

    @Mock
    private SetEmployeeBranchesUseCase setUseCase;
    @Mock
    private GetEmployeeBranchesUseCase getUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @InjectMocks
    private SetEmployeeBranchesAdapter adapter;

    @SuppressWarnings("unchecked")
    private void ejecutarElRunner() {
        when(systemAuthRunner.call(any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<Object>) inv.getArgument(0)).get());
    }

    @Test
    @DisplayName("pide TODAS las sedes de la empresa, no una id concreta")
    void pide_todas_las_sedes_de_la_empresa() {
        ejecutarElRunner();
        when(getUseCase.execute(55L, 9L)).thenReturn(new EmployeeBranchesDto(55L, List.of(7L)));

        adapter.assignAllCompanyBranches(55L, 9L);

        ArgumentCaptor<SetEmployeeBranchesCommand> captor = ArgumentCaptor
                .forClass(SetEmployeeBranchesCommand.class);
        verify(setUseCase).execute(captor.capture());
        assertThat(captor.getValue().employeeId()).isEqualTo(55L);
        assertThat(captor.getValue().companyId()).isEqualTo(9L);
        // allBranches = true: el dueño opera en toda su empresa, y hereda las sedes que
        // se creen despues (findFullCoverageEmployeeIds).
        assertThat(captor.getValue().allBranches()).isTrue();
        assertThat(captor.getValue().branchIds()).isNull();
    }

    /**
     * El nucleo del arreglo: lo que se devuelve es lo que quedo ESCRITO, releido de
     * {@code employee_branches}, no el objetivo que se pidio. El
     * {@code INSERT … SELECT} que materializa la asignacion no produce ninguna fila
     * —y no lanza— si el empleado o la sede no son de la empresa, asi que devolver
     * el objetivo convertiria la guarda del alta en un adorno.
     */
    @Test
    @DisplayName("devuelve las sedes releidas de la base, no las que se pidieron")
    void devuelve_las_sedes_releidas_de_la_base() {
        ejecutarElRunner();
        when(getUseCase.execute(55L, 9L)).thenReturn(new EmployeeBranchesDto(55L, List.of()));

        assertThat(adapter.assignAllCompanyBranches(55L, 9L)).isEmpty();
    }

    @Test
    @DisplayName("nada ocurre fuera del contexto de sistema")
    void nada_ocurre_fuera_del_contexto_de_sistema() {
        adapter.assignAllCompanyBranches(55L, 9L);

        verify(setUseCase, never()).execute(any());
        verify(getUseCase, never()).execute(any(), any());
        verify(systemAuthRunner).call(any(Supplier.class));
    }
}
