package com.vetsoftware.app.productcategory.application.port.out;

import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository {
    ProductCategory save(ProductCategory productCategory);
    Optional<ProductCategory> findById(Long id);
    Optional<ProductCategory> findByIdAndCompanyId(Long id, Long companyId);

    /** ¿Existe ya una categoría de producto ACTIVA con este name en la empresa? (unicidad de nombre por empresa) */
    boolean existsByCompanyIdAndName(Long companyId, String name);

    /** Igual, excluyendo la propia categoría (para validar en actualización). */
    boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id);
    List<ProductCategory> findAll();
    List<ProductCategory> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id, Long companyId);
}
