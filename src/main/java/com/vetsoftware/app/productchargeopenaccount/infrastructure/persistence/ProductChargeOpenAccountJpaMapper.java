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
                                                   EmployeeJpaEntity createdBy) {
        ProductChargeOpenAccountJpaEntity entity = new ProductChargeOpenAccountJpaEntity();
        entity.setId(charge.getId());
        entity.setAnimal(animal);
        entity.setProduct(product);
        entity.setOpenAccount(openAccount);
        entity.setCreatedBy(createdBy);
        entity.setCreatedDate(charge.getCreatedDate());
        entity.setEnabled(charge.isEnabled());
        return entity;
    }

    public ProductChargeOpenAccount toDomain(ProductChargeOpenAccountJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        ProductJpaEntity p = entity.getProduct();
        OpenAccountJpaEntity o = entity.getOpenAccount();
        EmployeeJpaEntity e = entity.getCreatedBy();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new ProductRef(p.getId(), p.getName(), p.getCode(), p.getSalePrice()),
            new OpenAccountRef(o.getId(), o.getCompany().getId()),
            e == null ? null : new EmployeeRef(e.getId(), e.getName()));
    }

    public ProductChargeOpenAccount toDomain(ProductChargeOpenAccountJpaEntity entity,
                                             AnimalRef animalRef,
                                             ProductRef productRef,
                                             OpenAccountRef openAccountRef,
                                             EmployeeRef createdByRef) {
        return new ProductChargeOpenAccount(
            entity.getId(),
            animalRef,
            productRef,
            openAccountRef,
            createdByRef,
            entity.getCreatedDate(),
            entity.isEnabled());
    }
}
