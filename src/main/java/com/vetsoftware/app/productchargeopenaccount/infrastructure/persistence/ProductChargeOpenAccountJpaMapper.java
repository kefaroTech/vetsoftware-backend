package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import org.springframework.stereotype.Component;

@Component
public class ProductChargeOpenAccountJpaMapper {

    public ProductChargeOpenAccountJpaEntity toJpa(ProductChargeOpenAccount charge,
                                                   AnimalJpaEntity animal,
                                                   ProductJpaEntity product,
                                                   OpenAccountJpaEntity openAccount,
                                                   EmployeeJpaEntity createdBy,
                                                   EmployeeJpaEntity voidedBy) {
        ProductChargeOpenAccountJpaEntity entity = new ProductChargeOpenAccountJpaEntity();
        entity.setId(charge.getId());
        entity.setAnimal(animal);
        entity.setProduct(product);
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

    public ProductChargeOpenAccount toDomain(ProductChargeOpenAccountJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        ProductJpaEntity p = entity.getProduct();
        OpenAccountJpaEntity o = entity.getOpenAccount();
        EmployeeJpaEntity e = entity.getCreatedBy();
        EmployeeJpaEntity v = entity.getVoidedBy();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new ProductRef(p.getId(), p.getName(), p.getCode(), p.getSalePrice()),
            new OpenAccountRef(o.getId(), o.getCompany().getId()),
            e == null ? null : new EmployeeRef(e.getId(), e.getName()),
            v == null ? null : new EmployeeRef(v.getId(), v.getName()));
    }

    public ProductChargeOpenAccount toDomain(ProductChargeOpenAccountJpaEntity entity,
                                             AnimalRef animalRef,
                                             ProductRef productRef,
                                             OpenAccountRef openAccountRef,
                                             EmployeeRef createdByRef,
                                             EmployeeRef voidedByRef) {
        return new ProductChargeOpenAccount(
            entity.getId(),
            animalRef,
            productRef,
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
