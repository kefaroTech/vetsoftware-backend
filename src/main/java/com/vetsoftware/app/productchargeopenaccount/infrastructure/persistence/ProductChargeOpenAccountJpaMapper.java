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
import com.vetsoftware.app.productchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductChargeOpenAccountJpaMapper {

    public ProductChargeOpenAccountJpaEntity toJpa(ProductChargeOpenAccount charge,
                                                   AnimalJpaEntity animal,
                                                   ProductJpaEntity product,
                                                   TaxJpaEntity tax,
                                                   OpenAccountJpaEntity openAccount,
                                                   EmployeeJpaEntity createdBy,
                                                   EmployeeJpaEntity voidedBy) {
        ProductChargeOpenAccountJpaEntity entity = new ProductChargeOpenAccountJpaEntity();
        entity.setId(charge.getId());
        entity.setAnimal(animal);
        entity.setProduct(product);
        entity.setUnitPrice(charge.getUnitPrice());
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
        return entity;
    }

    public ProductChargeOpenAccount toDomain(ProductChargeOpenAccountJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        ProductJpaEntity p = entity.getProduct();
        TaxJpaEntity t = entity.getTax();
        OpenAccountJpaEntity o = entity.getOpenAccount();
        EmployeeJpaEntity e = entity.getCreatedBy();
        EmployeeJpaEntity v = entity.getVoidedBy();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new ProductRef(p.getId(), p.getName(), p.getCode(), p.getSalePrice()),
            t == null ? null : new TaxRef(t.getId(), t.getName(), t.getPercentage(),
                t.getTaxScheme() == null ? null : t.getTaxScheme().name()),
            new OpenAccountRef(o.getId(), o.getCompany().getId()),
            e == null ? null : new EmployeeRef(e.getId(), e.getName()),
            v == null ? null : new EmployeeRef(v.getId(), v.getName()));
    }

    public ProductChargeOpenAccount toDomain(ProductChargeOpenAccountJpaEntity entity,
                                             AnimalRef animalRef,
                                             ProductRef productRef,
                                             TaxRef taxRef,
                                             OpenAccountRef openAccountRef,
                                             EmployeeRef createdByRef,
                                             EmployeeRef voidedByRef) {
        return new ProductChargeOpenAccount(
            entity.getId(),
            animalRef,
            productRef,
            entity.getUnitPrice(),
            taxRef,
            entity.isHasTax(),
            entity.getTaxPercentage(),
            entity.getTaxName(),
            entity.getTaxScheme(),
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
            entity.getVoidReason());
    }
}
