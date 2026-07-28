package com.vetsoftware.app.employeebranch.application.usecase;

import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.application.port.out.BranchAccessCachePort;
import com.vetsoftware.app.employeebranch.application.port.out.BranchQueryPort;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeBranchRepository;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeQueryPort;
import io.micrometer.observation.annotation.Observed;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.branch.set")
@Service
public class SetEmployeeBranchesService implements SetEmployeeBranchesUseCase {
    private final EmployeeBranchRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final BranchAccessCachePort cachePort;

    public SetEmployeeBranchesService(EmployeeBranchRepository repository,
                                      EmployeeQueryPort employeeQueryPort,
                                      BranchQueryPort branchQueryPort,
                                      BranchAccessCachePort cachePort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.branchQueryPort = branchQueryPort;
        this.cachePort = cachePort;
    }

    @Override
    @Transactional
    public EmployeeBranchesDto execute(SetEmployeeBranchesCommand command) {
        if (!employeeQueryPort.existsByIdAndCompanyId(command.employeeId(), command.companyId())) {
            throw new IllegalArgumentException("Employee not found: " + command.employeeId());
        }

        List<Long> companyBranchIds = branchQueryPort.findBranchIdsByCompanyId(command.companyId());
        Set<Long> target = new LinkedHashSet<>();
        if (command.allBranches()) {
            target.addAll(companyBranchIds);
        } else if (command.branchIds() != null) {
            for (Long branchId : command.branchIds()) {
                if (!companyBranchIds.contains(branchId)) {
                    throw new IllegalArgumentException("Branch not found: " + branchId);
                }
                target.add(branchId);
            }
        }
        // Regla: un empleado debe quedar con al menos una sede (para revocar acceso se desactiva el empleado).
        if (target.isEmpty()) {
            throw new IllegalArgumentException("At least one branch is required");
        }

        repository.replaceBranches(command.employeeId(), target);
        cachePort.evictByEmployeeId(command.employeeId());
        return new EmployeeBranchesDto(command.employeeId(), List.copyOf(target));
    }
}
