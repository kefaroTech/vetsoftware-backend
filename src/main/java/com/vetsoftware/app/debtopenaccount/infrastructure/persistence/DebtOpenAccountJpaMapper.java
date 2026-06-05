package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DebtOpenAccountJpaMapper {

    public DebtOpenAccountJpaEntity toJpa(DebtOpenAccount debtOpenAccount,
                                          OpenAccountJpaEntity openAccount,
                                          EmployeeJpaEntity createdBy) {
        DebtOpenAccountJpaEntity entity = new DebtOpenAccountJpaEntity();
        entity.setId(debtOpenAccount.getId());
        entity.setAmount(debtOpenAccount.getAmount());
        entity.setPaymentMethod(debtOpenAccount.getPaymentMethod());
        entity.setOpenAccount(openAccount);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(debtOpenAccount.getCreatedDate());
        entity.setEnabled(debtOpenAccount.isEnabled());
        return entity;
    }

    public DebtOpenAccount toDomain(DebtOpenAccountJpaEntity entity) {
        OpenAccountJpaEntity oa = entity.getOpenAccount();
        EmployeeJpaEntity e = entity.getCreatedBy();
        return toDomain(entity,
            new OpenAccountRef(oa.getId(), oa.getCompany().getId()),
            new EmployeeRef(e.getId(), e.getName()));
    }

    public DebtOpenAccount toDomain(DebtOpenAccountJpaEntity entity,
                                    OpenAccountRef openAccountRef, EmployeeRef createdByRef) {
        return new DebtOpenAccount(
            entity.getId(),
            entity.getAmount(),
            entity.getPaymentMethod(),
            openAccountRef,
            createdByRef,
            entity.getCreatedDate(),
            entity.isEnabled());
    }
}
