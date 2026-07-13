package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.employee.application.port.out.EmployeeBranchesQueryPort;
import com.vetsoftware.app.employee.domain.BranchRef;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchAssignmentView;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeBranchesQueryPort implements EmployeeBranchesQueryPort {
    private final EmployeeBranchJpaRepository employeeBranchJpaRepository;

    public JpaEmployeeBranchesQueryPort(EmployeeBranchJpaRepository employeeBranchJpaRepository) {
        this.employeeBranchJpaRepository = employeeBranchJpaRepository;
    }

    @Override
    public Map<Long, List<BranchRef>> findBranchesByEmployeeIds(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) return Map.of();
        return employeeBranchJpaRepository.findAssignmentsByEmployeeIds(employeeIds).stream()
            .collect(Collectors.groupingBy(
                EmployeeBranchAssignmentView::getEmployeeId,
                Collectors.mapping(
                    v -> new BranchRef(v.getBranchId(), v.getBranchName()),
                    Collectors.collectingAndThen(Collectors.toList(), list -> {
                        list.sort(Comparator.comparing(BranchRef::name, String.CASE_INSENSITIVE_ORDER));
                        return list;
                    })
                )
            ));
    }
}
