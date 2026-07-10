package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
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
        entity.setCompany(company);
        entity.setCreatedDate(employee.getCreatedDate());
        entity.setEnabled(employee.isEnabled());
        entity.setEmailVerified(employee.isEmailVerified());
        entity.setMustChangePassword(employee.isMustChangePassword());
        entity.setAuthVersion(employee.getAuthVersion());
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
            companyRef,
            entity.getCreatedDate(),
            entity.isEnabled(),
            entity.isEmailVerified(),
            entity.isMustChangePassword(),
            entity.getAuthVersion()
        );
    }
}
