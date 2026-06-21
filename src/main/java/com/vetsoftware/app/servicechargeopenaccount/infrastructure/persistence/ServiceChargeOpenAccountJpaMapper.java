package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceChargeOpenAccountJpaMapper {

    public ServiceChargeOpenAccountJpaEntity toJpa(ServiceChargeOpenAccount charge,
                                                   AnimalJpaEntity animal,
                                                   ServiceJpaEntity service,
                                                   TaxJpaEntity tax,
                                                   OpenAccountJpaEntity openAccount,
                                                   EmployeeJpaEntity createdBy,
                                                   EmployeeJpaEntity voidedBy) {
        ServiceChargeOpenAccountJpaEntity entity = new ServiceChargeOpenAccountJpaEntity();
        entity.setId(charge.getId());
        entity.setAnimal(animal);
        entity.setService(service);
        entity.setUnitPrice(charge.getUnitPrice());
        entity.setTax(tax);
        entity.setHasTax(charge.isHasTax());
        entity.setTaxPercentage(charge.getTaxPercentage());
        entity.setTaxName(charge.getTaxName());
        entity.setTaxScheme(charge.getTaxScheme());
        entity.setTaxTreatment(charge.getTaxTreatment());
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

    public ServiceChargeOpenAccount toDomain(ServiceChargeOpenAccountJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        ServiceJpaEntity s = entity.getService();
        TaxJpaEntity t = entity.getTax();
        OpenAccountJpaEntity o = entity.getOpenAccount();
        EmployeeJpaEntity e = entity.getCreatedBy();
        EmployeeJpaEntity v = entity.getVoidedBy();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new ServiceRef(s.getId(), s.getName(), s.getPrice()),
            t == null ? null : new TaxRef(t.getId(), t.getName(), t.getPercentage(),
                t.getTaxScheme() == null ? null : t.getTaxScheme().name()),
            new OpenAccountRef(o.getId(), o.getCompany().getId()),
            e == null ? null : new EmployeeRef(e.getId(), e.getName()),
            v == null ? null : new EmployeeRef(v.getId(), v.getName()));
    }

    public ServiceChargeOpenAccount toDomain(ServiceChargeOpenAccountJpaEntity entity,
                                             AnimalRef animalRef, ServiceRef serviceRef, TaxRef taxRef,
                                             OpenAccountRef openAccountRef, EmployeeRef createdByRef,
                                             EmployeeRef voidedByRef) {
        return new ServiceChargeOpenAccount(
            entity.getId(),
            animalRef,
            serviceRef,
            entity.getUnitPrice(),
            taxRef,
            entity.isHasTax(),
            entity.getTaxPercentage(),
            entity.getTaxName(),
            entity.getTaxScheme(),
            entity.getTaxTreatment(),
            entity.getBaseAmount(),
            entity.getTaxAmount(),
            entity.getTotalAmount(),
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
