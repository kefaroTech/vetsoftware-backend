package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeJpaMapper {
    public EmployeeJpaEntity toJpa(Employee employee, CompanyJpaEntity company) {
        EmployeeJpaEntity entity = new EmployeeJpaEntity();
        entity.setId(employee.getId());
        entity.setEmployeeCode(employee.getEmployeeCode());
        entity.setHashPassword(employee.getHashPassword());
        entity.setName(employee.getName());
        entity.setEmail(employee.getEmail());
        entity.setStatus(employee.getStatus().name());
        entity.setCompany(company);
        entity.setCreatedDate(employee.getCreatedDate());
        return entity;
    }

    public Employee toDomain(EmployeeJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity, new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Employee toDomain(EmployeeJpaEntity entity, CompanyRef companyRef) {
        return new Employee(
            entity.getId(),
            entity.getEmployeeCode(),
            entity.getHashPassword(),
            entity.getName(),
            entity.getEmail(),
            EmployeeStatus.valueOf(entity.getStatus()),
            companyRef,
            entity.getCreatedDate()
        );
    }
}
