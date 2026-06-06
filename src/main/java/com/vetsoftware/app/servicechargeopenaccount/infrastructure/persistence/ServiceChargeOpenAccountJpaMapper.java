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
import org.springframework.stereotype.Component;

@Component
public class ServiceChargeOpenAccountJpaMapper {

    public ServiceChargeOpenAccountJpaEntity toJpa(ServiceChargeOpenAccount charge,
                                                   AnimalJpaEntity animal,
                                                   ServiceJpaEntity service,
                                                   OpenAccountJpaEntity openAccount,
                                                   EmployeeJpaEntity createdBy,
                                                   EmployeeJpaEntity voidedBy) {
        ServiceChargeOpenAccountJpaEntity entity = new ServiceChargeOpenAccountJpaEntity();
        entity.setId(charge.getId());
        entity.setAnimal(animal);
        entity.setService(service);
        entity.setUnitPrice(charge.getUnitPrice());
        entity.setOpenAccount(openAccount);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(charge.getCreatedDate());
        entity.setEnabled(charge.isEnabled());
        entity.setVoided(charge.isVoided());
        entity.setVoidedBy(voidedBy);
        entity.setVoidedAt(charge.getVoidedAt());
        entity.setVoidReason(charge.getVoidReason());
        return entity;
    }

    public ServiceChargeOpenAccount toDomain(ServiceChargeOpenAccountJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        ServiceJpaEntity s = entity.getService();
        OpenAccountJpaEntity o = entity.getOpenAccount();
        EmployeeJpaEntity e = entity.getCreatedBy();
        EmployeeJpaEntity v = entity.getVoidedBy();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new ServiceRef(s.getId(), s.getName(), s.getPrice()),
            new OpenAccountRef(o.getId(), o.getCompany().getId()),
            e == null ? null : new EmployeeRef(e.getId(), e.getName()),
            v == null ? null : new EmployeeRef(v.getId(), v.getName()));
    }

    public ServiceChargeOpenAccount toDomain(ServiceChargeOpenAccountJpaEntity entity,
                                             AnimalRef animalRef, ServiceRef serviceRef,
                                             OpenAccountRef openAccountRef, EmployeeRef createdByRef,
                                             EmployeeRef voidedByRef) {
        return new ServiceChargeOpenAccount(
            entity.getId(),
            animalRef,
            serviceRef,
            entity.getUnitPrice(),
            openAccountRef,
            createdByRef,
            entity.getCreatedDate(),
            entity.isEnabled(),
            entity.isVoided(),
            voidedByRef,
            entity.getVoidedAt(),
            entity.getVoidReason());
    }
}
