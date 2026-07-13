package com.vetsoftware.app.product.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.TaxRef;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductJpaMapper {

    public ProductJpaEntity toJpa(Product product,
                                  ProductCategoryJpaEntity productCategory,
                                  TaxJpaEntity tax,
                                  CompanyJpaEntity company) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());
        entity.setCode(product.getCode());
        entity.setSalePrice(product.getSalePrice());
        entity.setProvider(product.getProvider());
        entity.setTaxTreatment(product.getTaxTreatment());
        entity.setNotes(product.getNotes());
        entity.setProductCategory(productCategory);
        entity.setTax(tax);
        entity.setCompany(company);
        entity.setCreatedDate(product.getCreatedDate());
        entity.setUpdatedDate(product.getUpdatedDate());
        entity.setUpdatedBy(product.getUpdatedBy());
        entity.setVersion(product.getVersion());
        entity.setEnabled(product.isEnabled());
        return entity;
    }

    public Product toDomain(ProductJpaEntity entity) {
        ProductCategoryJpaEntity pc = entity.getProductCategory();
        TaxJpaEntity t = entity.getTax();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new ProductCategoryRef(pc.getId(), pc.getName()),
            t == null ? null : new TaxRef(t.getId(), t.getName(), t.getPercentage()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Product toDomain(ProductJpaEntity entity, ProductCategoryRef productCategoryRef,
                            TaxRef taxRef, CompanyRef companyRef) {
        return new Product(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getSalePrice(),
            entity.getProvider(),
            entity.getTaxTreatment(),
            entity.getNotes(),
            productCategoryRef,
            taxRef,
            companyRef,
            entity.getCreatedDate(),
            entity.getUpdatedDate(),
            entity.getUpdatedBy(),
            entity.getVersion(),
            entity.isEnabled());
    }
}
