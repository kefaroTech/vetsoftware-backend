package com.vetsoftware.app.employeerole.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.RoleRef;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EmployeeRoleJpaMapper {

    public EmployeeRoleJpaEntity toJpa(EmployeeRole employeeRole, EmployeeJpaEntity employee,
            RoleJpaEntity role) {
        EmployeeRoleJpaEntity entity = new EmployeeRoleJpaEntity();
        entity.setId(employeeRole.getId());
        entity.setEmployee(employee);
        entity.setRole(role);
        entity.setCreatedDate(employeeRole.getCreatedDate());
        entity.setEnabled(employeeRole.isEnabled());
        return entity;
    }

    public EmployeeRole toDomain(EmployeeRoleJpaEntity entity) {
        EmployeeJpaEntity e = entity.getEmployee();
        RoleJpaEntity r = entity.getRole();
        return toDomain(entity, new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()),
                new RoleRef(r.getId(), r.getName(), r.getCode()));
    }

    public EmployeeRole toDomain(EmployeeRoleJpaEntity entity, EmployeeRef employeeRef,
            RoleRef roleRef) {
        return new EmployeeRole(entity.getId(), employeeRef, roleRef, entity.getCreatedDate(),
                entity.isEnabled());
    }
}
