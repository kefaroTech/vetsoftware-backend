package com.vetsoftware.app.employeebranch.testsupport;

import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import java.util.List;

/** Fixtures de la feature de sedes por empleado (asignacion multi-sucursal). */
public final class EmployeeBranchMother {

    public static final Long EMPLOYEE_ID = 940L;
    public static final Long COMPANY_ID = 900L;
    public static final Long BRANCH_1 = 910L;
    public static final Long BRANCH_2 = 911L;
    public static final Long BRANCH_3 = 912L;

    private EmployeeBranchMother() {
    }

    /** Comando con un set explicito de sedes (allBranches=false). */
    public static SetEmployeeBranchesCommand comandoConSedes(Long... branchIds) {
        return new SetEmployeeBranchesCommand(EMPLOYEE_ID, COMPANY_ID, false, List.of(branchIds));
    }

    /** Comando que pide todas las sedes de la empresa. */
    public static SetEmployeeBranchesCommand comandoTodasLasSedes() {
        return new SetEmployeeBranchesCommand(EMPLOYEE_ID, COMPANY_ID, true, null);
    }
}
