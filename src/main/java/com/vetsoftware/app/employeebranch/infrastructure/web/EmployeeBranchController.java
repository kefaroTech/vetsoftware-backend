package com.vetsoftware.app.employeebranch.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.in.GetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.infrastructure.web.request.SetEmployeeBranchesRequest;
import com.vetsoftware.app.employeebranch.infrastructure.web.response.EmployeeBranchesResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión del alcance por sede de un empleado (multi-sucursal). El {@code companyId} lo deriva el backend del
 * contexto (nunca lo elige el cliente); el ownership se valida en el {@code @PreAuthorize} de cada use case.
 */
@RestController
@RequestMapping("/employees/{employeeId}/branches")
public class EmployeeBranchController {

    private final GetEmployeeBranchesUseCase getUseCase;
    private final SetEmployeeBranchesUseCase setUseCase;
    private final Authz authz;

    public EmployeeBranchController(GetEmployeeBranchesUseCase getUseCase,
                                    SetEmployeeBranchesUseCase setUseCase,
                                    Authz authz) {
        this.getUseCase = getUseCase;
        this.setUseCase = setUseCase;
        this.authz = authz;
    }

    @GetMapping
    public EmployeeBranchesResponse get(@PathVariable Long employeeId) {
        EmployeeBranchesDto dto = getUseCase.execute(employeeId, authz.currentCompanyId());
        return new EmployeeBranchesResponse(dto.employeeId(), dto.branchIds());
    }

    @PutMapping
    public EmployeeBranchesResponse set(@PathVariable Long employeeId,
                                        @Valid @RequestBody SetEmployeeBranchesRequest request) {
        boolean allBranches = request.allBranches();
        List<Long> branchIds = request.branchIds();
        // Un no-admin solo puede asignar sedes que él mismo tiene: "todas" = todas SUS sedes (no toda la empresa);
        // si vienen explícitas, cada una debe estar en su alcance. Un admin gestiona toda la empresa (sin acotar).
        if (!authz.isAdmin()) {
            if (allBranches) {
                allBranches = false;
                branchIds = new ArrayList<>(authz.currentBranchIds());
            } else {
                authz.requireAssignableBranches(branchIds);
            }
        }
        EmployeeBranchesDto dto = setUseCase.execute(new SetEmployeeBranchesCommand(
            employeeId, authz.currentCompanyId(), allBranches, branchIds));
        return new EmployeeBranchesResponse(dto.employeeId(), dto.branchIds());
    }
}
