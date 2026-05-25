package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaEntity;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeRolesQueryPort implements EmployeeRolesQueryPort {
    private final EmployeeRoleJpaRepository employeeRoleJpaRepository;

    public JpaEmployeeRolesQueryPort(EmployeeRoleJpaRepository employeeRoleJpaRepository) {
        this.employeeRoleJpaRepository = employeeRoleJpaRepository;
    }

    @Override
    public Map<Long, List<RoleSnapshot>> findRolesByEmployeeIds(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) return Map.of();
        return employeeRoleJpaRepository.findByEmployeeIdIn(employeeIds).stream()
            .collect(Collectors.groupingBy(
                er -> er.getEmployee().getId(),
                Collectors.mapping(this::toSnapshot, Collectors.toList())
            ));
    }

    private RoleSnapshot toSnapshot(EmployeeRoleJpaEntity er) {
        RoleJpaEntity r = er.getRole();
        return new RoleSnapshot(er.getId(), r.getId(), r.getName(), r.getCode());
    }
}
