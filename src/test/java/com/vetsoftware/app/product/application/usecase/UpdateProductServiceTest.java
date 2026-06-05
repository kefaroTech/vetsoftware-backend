package com.vetsoftware.app.product.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductCategoryQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.application.port.out.TaxQueryPort;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.product.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateProductServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ProductCategoryRef category = new ProductCategoryRef(3L, "Alimentos");
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));

    private Product existing() {
        return new Product(1L, "Croqueta", "P-001", new BigDecimal("10.00"), new BigDecimal("15.00"),
                100, 10, "Proveedor", true, false, "notas", category, tax, company,
                LocalDateTime.now(), true);
    }

    private ProductRepository repositoryWith(Product stored) {
        return new ProductRepository() {
            Product current = stored;
            @Override public Product save(Product product) { current = product; return product; }
            @Override public Optional<Product> findById(Long id) { return Optional.ofNullable(current); }
            @Override public List<Product> findAll() { return List.of(); }
            @Override public List<Product> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public PageResult<Product> search(SearchProductsCommand command) {
                return new PageResult<>(List.of(), 0, 20, 0, 0);
            }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) { return companyId -> result; }
    private ProductCategoryQueryPort categoryQueryPort(Optional<ProductCategoryRef> result) { return id -> result; }
    private TaxQueryPort taxQueryPort(Optional<TaxRef> result) { return id -> result; }

    private UpdateProductCommand command(Long taxId) {
        return new UpdateProductCommand(1L, "Croqueta Premium", "P-002", new BigDecimal("12.00"),
                new BigDecimal("20.00"), 50, 5, "Otro", false, true, "actualizado",
                3L, taxId, 5L);
    }

    @Test
    void updates_existing_product() {
        var service = new UpdateProductService(repositoryWith(existing()),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        ProductDto dto = service.execute(command(7L));

        assertEquals("Croqueta Premium", dto.name());
        assertEquals("P-002", dto.code());
        assertEquals(new BigDecimal("20.00"), dto.salePrice());
        assertEquals(7L, dto.tax().id());
    }

    @Test
    void updates_clearing_tax() {
        var service = new UpdateProductService(repositoryWith(existing()),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.empty()));

        ProductDto dto = service.execute(command(null));

        assertNull(dto.tax());
    }

    @Test
    void fails_when_product_not_found() {
        var service = new UpdateProductService(repositoryWith(null),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        assertThrows(ProductNotFoundException.class, () -> service.execute(command(7L)));
    }

    @Test
    void fails_when_category_not_found() {
        var service = new UpdateProductService(repositoryWith(existing()),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.empty()),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L)));
    }
}
