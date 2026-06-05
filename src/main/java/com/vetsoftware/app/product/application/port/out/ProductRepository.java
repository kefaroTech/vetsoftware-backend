package com.vetsoftware.app.product.application.port.out;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAll();
    List<Product> findAllByCompanyId(Long companyId);
    PageResult<Product> search(SearchProductsCommand command);
    void delete(Long id);
    int reactivate(Long id);
}
