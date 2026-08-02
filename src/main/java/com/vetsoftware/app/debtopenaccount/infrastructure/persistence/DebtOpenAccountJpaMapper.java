package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DebtOpenAccountJpaMapper {

  public DebtOpenAccountJpaEntity toJpa(
      DebtOpenAccount debtOpenAccount,
      OpenAccountJpaEntity openAccount,
      EmployeeJpaEntity createdBy,
      EmployeeJpaEntity voidedBy) {
    DebtOpenAccountJpaEntity entity = new DebtOpenAccountJpaEntity();
    entity.setId(debtOpenAccount.getId());
    entity.setAmount(debtOpenAccount.getAmount());
    entity.setPaymentMethod(debtOpenAccount.getPaymentMethod());
    entity.setOpenAccount(openAccount);
    entity.setCreatedBy(createdBy);
    entity.setCreatedDate(debtOpenAccount.getCreatedDate());
    entity.setEnabled(debtOpenAccount.isEnabled());
    entity.setVoided(debtOpenAccount.isVoided());
    entity.setVoidedBy(voidedBy);
    entity.setVoidedAt(debtOpenAccount.getVoidedAt());
    entity.setVoidReason(debtOpenAccount.getVoidReason());
    entity.setClientRequestId(debtOpenAccount.getClientRequestId());
    return entity;
  }

  public DebtOpenAccount toDomain(DebtOpenAccountJpaEntity entity) {
    OpenAccountJpaEntity oa = entity.getOpenAccount();
    EmployeeJpaEntity e = entity.getCreatedBy();
    EmployeeJpaEntity v = entity.getVoidedBy();
    EmployeeRef voidedByRef = v == null ? null : new EmployeeRef(v.getId(), v.getName());
    return toDomain(
        entity,
        new OpenAccountRef(oa.getId(), oa.getCompany().getId()),
        new EmployeeRef(e.getId(), e.getName()),
        voidedByRef);
  }

  public DebtOpenAccount toDomain(
      DebtOpenAccountJpaEntity entity,
      OpenAccountRef openAccountRef,
      EmployeeRef createdByRef,
      EmployeeRef voidedByRef) {
    return new DebtOpenAccount(
        entity.getId(),
        entity.getAmount(),
        entity.getPaymentMethod(),
        openAccountRef,
        createdByRef,
        entity.getCreatedDate(),
        entity.isEnabled(),
        entity.isVoided(),
        voidedByRef,
        entity.getVoidedAt(),
        entity.getVoidReason(),
        entity.getClientRequestId());
  }
}
