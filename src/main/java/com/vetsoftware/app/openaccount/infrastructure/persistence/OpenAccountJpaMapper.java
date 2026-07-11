package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OpenAccountJpaMapper {

    public OpenAccountJpaEntity toJpa(OpenAccount openAccount,
                                      OwnerJpaEntity owner,
                                      CompanyJpaEntity company,
                                      BranchJpaEntity branch,
                                      EmployeeJpaEntity createdBy,
                                      EmployeeJpaEntity closedBy) {
        OpenAccountJpaEntity entity = new OpenAccountJpaEntity();
        entity.setId(openAccount.getId());
        entity.setOwner(owner);
        entity.setTotalAmount(openAccount.getTotalAmount());
        entity.setPaidAmount(openAccount.getPaidAmount());
        entity.setOutstandingAmount(openAccount.getOutstandingAmount());
        entity.setCompany(company);
        entity.setBranch(branch);
        entity.setStatus(openAccount.getStatus());
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(openAccount.getCreatedDate());
        entity.setEnabled(openAccount.isEnabled());
        entity.setClosedBy(closedBy);
        entity.setClosedAt(openAccount.getClosedAt());
        entity.setCloseReason(openAccount.getCloseReason());
        entity.setReversed(openAccount.isReversed());
        entity.setReversedAt(openAccount.getReversedAt());
        entity.setVersion(openAccount.getVersion());
        return entity;
    }

    public OpenAccount toDomain(OpenAccountJpaEntity entity) {
        OwnerJpaEntity o = entity.getOwner();
        CompanyJpaEntity c = entity.getCompany();
        BranchJpaEntity b = entity.getBranch();
        EmployeeJpaEntity cb = entity.getCreatedBy();
        EmployeeJpaEntity closedBy = entity.getClosedBy();
        EmployeeRef closedByRef = closedBy != null
            ? new EmployeeRef(closedBy.getId(), closedBy.getName())
            : null;
        return toDomain(entity,
            new OwnerRef(o.getId(), o.getName(), o.getDocument()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()),
            new BranchRef(b.getId(), b.getName(), b.getCode()),
            new EmployeeRef(cb.getId(), cb.getName()),
            closedByRef);
    }

    public OpenAccount toDomain(OpenAccountJpaEntity entity, OwnerRef ownerRef,
                                CompanyRef companyRef, BranchRef branchRef, EmployeeRef createdByRef,
                                EmployeeRef closedByRef) {
        return new OpenAccount(
            entity.getId(),
            ownerRef,
            entity.getTotalAmount(),
            entity.getPaidAmount(),
            entity.getOutstandingAmount(),
            companyRef,
            branchRef,
            entity.getStatus(),
            createdByRef,
            entity.getCreatedDate(),
            entity.isEnabled(),
            closedByRef,
            entity.getClosedAt(),
            entity.getCloseReason(),
            entity.isReversed(),
            entity.getReversedAt(),
            entity.getVersion());
    }
}
