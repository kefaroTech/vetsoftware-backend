package com.vetsoftware.app.productcategory.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryJpaMapper {
    public ProductCategoryJpaEntity toJpa(ProductCategory productCategory, CompanyJpaEntity company) {
        ProductCategoryJpaEntity entity = new ProductCategoryJpaEntity();
        entity.setId(productCategory.getId());
        entity.setName(productCategory.getName());
        entity.setDescription(productCategory.getDescription());
        entity.setCompany(company);
        entity.setCreatedDate(productCategory.getCreatedDate());
        entity.setUpdatedDate(productCategory.getUpdatedDate());
        entity.setUpdatedBy(productCategory.getUpdatedBy());
        entity.setEnabled(productCategory.isEnabled());
        return entity;
    }

    public ProductCategory toDomain(ProductCategoryJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
                c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public ProductCategory toDomain(ProductCategoryJpaEntity entity, CompanyRef companyRef) {
        return new ProductCategory(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                companyRef,
                entity.getCreatedDate(),
                entity.getUpdatedDate(),
                entity.getUpdatedBy(),
                entity.isEnabled());
    }
}
