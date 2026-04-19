package com.vetsoftware.app.infrastructure.persistence;

import com.vetsoftware.app.domain.Employee;
import com.vetsoftware.app.domain.EmployeeStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeJpaMapper {
    public EmployeeJpaEntity toJpa(Employee employee) {
        EmployeeJpaEntity entity = new EmployeeJpaEntity();
        entity.setId(employee.getId());
        entity.setEmployeeCode(employee.getEmployeeCode());
        entity.setHashPassword(employee.getHashPassword());
        entity.setName(employee.getName());
        entity.setEmail(employee.getEmail());
        entity.setStatus(employee.getStatus().name());
        entity.setCompanyId(employee.getCompanyId());
        entity.setCreatedDate(employee.getCreatedDate());
        entity.setCreatedBy(employee.getCreatedBy());
        return entity;
    }

    public Employee toDomain(EmployeeJpaEntity entity) {
        return new Employee(
            entity.getId(),
            entity.getEmployeeCode(),
            entity.getHashPassword(),
            entity.getName(),
            entity.getEmail(),
            EmployeeStatus.valueOf(entity.getStatus()),
            entity.getCompanyId(),
            entity.getCreatedDate(),
            entity.getCreatedBy()
        );
    }
}
