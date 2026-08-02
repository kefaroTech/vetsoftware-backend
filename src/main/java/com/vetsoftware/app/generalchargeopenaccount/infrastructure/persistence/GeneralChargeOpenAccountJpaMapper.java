package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GeneralChargeOpenAccountJpaMapper {

    public GeneralChargeOpenAccountJpaEntity toJpa(GeneralChargeOpenAccount charge,
            TaxJpaEntity tax, OpenAccountJpaEntity openAccount, EmployeeJpaEntity createdBy,
            EmployeeJpaEntity voidedBy) {
        GeneralChargeOpenAccountJpaEntity entity = new GeneralChargeOpenAccountJpaEntity();
        entity.setId(charge.getId());
        entity.setName(charge.getName());
        entity.setUnitAmount(charge.getUnitAmount());
        entity.setQuantity(charge.getQuantity());
        entity.setTax(tax);
        entity.setHasTax(charge.isHasTax());
        entity.setTaxPercentage(charge.getTaxPercentage());
        entity.setTaxName(charge.getTaxName());
        entity.setTaxScheme(charge.getTaxScheme());
        entity.setBaseAmount(charge.getBaseAmount());
        entity.setTaxAmount(charge.getTaxAmount());
        entity.setTotalAmount(charge.getTotalAmount());
        entity.setOpenAccount(openAccount);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(charge.getCreatedDate());
        entity.setEnabled(charge.isEnabled());
        entity.setVoided(charge.isVoided());
        entity.setVoidedBy(voidedBy);
        entity.setVoidedAt(charge.getVoidedAt());
        entity.setVoidReason(charge.getVoidReason());
        entity.setClientRequestId(charge.getClientRequestId());
        return entity;
    }

    public GeneralChargeOpenAccount toDomain(GeneralChargeOpenAccountJpaEntity entity) {
        TaxJpaEntity t = entity.getTax();
        OpenAccountJpaEntity oa = entity.getOpenAccount();
        EmployeeJpaEntity emp = entity.getCreatedBy();
        EmployeeJpaEntity v = entity.getVoidedBy();
        return toDomain(entity,
                t == null
                        ? null
                        : new TaxRef(t.getId(), t.getName(), t.getPercentage(),
                                t.getTaxScheme() == null ? null : t.getTaxScheme().name()),
                new OpenAccountRef(oa.getId(), oa.getCompany().getId()),
                new EmployeeRef(emp.getId(), emp.getName()),
                v == null ? null : new EmployeeRef(v.getId(), v.getName()));
    }

    public GeneralChargeOpenAccount toDomain(GeneralChargeOpenAccountJpaEntity entity,
            TaxRef taxRef, OpenAccountRef openAccountRef, EmployeeRef createdByRef,
            EmployeeRef voidedByRef) {
        return new GeneralChargeOpenAccount(entity.getId(), entity.getName(),
                entity.getUnitAmount(), entity.getQuantity(), taxRef, entity.isHasTax(),
                entity.getTaxPercentage(), entity.getTaxName(), entity.getTaxScheme(),
                entity.getBaseAmount(), entity.getTaxAmount(), entity.getTotalAmount(),
                openAccountRef, createdByRef, entity.getCreatedDate(), entity.isEnabled(),
                entity.isVoided(), voidedByRef, entity.getVoidedAt(), entity.getVoidReason(),
                entity.getClientRequestId());
    }
}
