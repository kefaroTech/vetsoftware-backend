package com.vetsoftware.app.product.application.port.out;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);

    /** ¿Existe ya un producto ACTIVO con este code en la empresa? (unicidad de SKU por empresa) */
    boolean existsByCompanyIdAndCode(Long companyId, String code);

    /** Igual, excluyendo el propio producto (para validar en actualización). */
    boolean existsByCompanyIdAndCodeExcludingId(Long companyId, String code, Long id);

    /** ¿Existe ya un producto ACTIVO con este name en la empresa? (unicidad de nombre por empresa) */
    boolean existsByCompanyIdAndName(Long companyId, String name);

    /** Igual, excluyendo el propio producto (para validar en actualización). */
    boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id);
    List<Product> findAll();
    List<Product> findAllByCompanyId(Long companyId);
    PageResult<Product> search(SearchProductsCommand command);
    void delete(Long id);
    int reactivate(Long id);
}
