package com.vetsoftware.app.product.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteProductServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ProductCategoryRef category = new ProductCategoryRef(3L, "Alimentos");

    private Product existing() {
        return new Product(1L, "Croqueta", "P-001", new BigDecimal("10.00"), new BigDecimal("15.00"),
                100, 10, null, false, false, null, category, null, company, LocalDateTime.now(), true);
    }

    private ProductRepository repositoryWith(Product existing, boolean[] deleted) {
        return new ProductRepository() {
            @Override public Product save(Product product) { return product; }
            @Override public Optional<Product> findById(Long id) { return Optional.ofNullable(existing); }
            @Override public List<Product> findAll() { return List.of(); }
            @Override public List<Product> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public PageResult<Product> search(SearchProductsCommand command) {
                return new PageResult<>(List.of(), 0, 20, 0, 0);
            }
            @Override public void delete(Long id) { deleted[0] = true; }
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    @Test
    void deletes_existing_product() {
        boolean[] deleted = {false};
        var service = new DeleteProductService(repositoryWith(existing(), deleted));

        service.execute(1L);

        assertTrue(deleted[0]);
    }

    @Test
    void fails_when_product_not_found() {
        boolean[] deleted = {false};
        var service = new DeleteProductService(repositoryWith(null, deleted));

        assertThrows(ProductNotFoundException.class, () -> service.execute(99L));
        assertFalse(deleted[0]);
    }
}
