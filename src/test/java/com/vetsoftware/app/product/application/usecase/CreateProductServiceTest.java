package com.vetsoftware.app.product.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.product.application.command.CreateProductCommand;
import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductCategoryQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.application.port.out.TaxQueryPort;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.TaxRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateProductServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ProductCategoryRef category = new ProductCategoryRef(3L, "Alimentos");
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));

    private Product savedProduct;

    private final ProductRepository repository = new ProductRepository() {
        @Override public Product save(Product product) {
            savedProduct = new Product(1L, product.getName(), product.getCode(),
                    product.getPurchasePrice(), product.getSalePrice(), product.getCurrentStock(),
                    product.getMinStock(), product.getProvider(), product.isHasTax(),
                    product.isExpireDate(), product.getNotes(), product.getProductCategory(),
                    product.getTax(), product.getCompany(), product.getCreatedDate(), product.isEnabled());
            return savedProduct;
        }
        @Override public Optional<Product> findById(Long id) { return Optional.ofNullable(savedProduct); }
        @Override public List<Product> findAll() { return List.of(); }
        @Override public List<Product> findAllByCompanyId(Long companyId) { return List.of(); }
        @Override public PageResult<Product> search(SearchProductsCommand command) {
            return new PageResult<>(List.of(), 0, 20, 0, 0);
        }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) { return companyId -> result; }
    private ProductCategoryQueryPort categoryQueryPort(Optional<ProductCategoryRef> result) { return id -> result; }
    private TaxQueryPort taxQueryPort(Optional<TaxRef> result) { return id -> result; }

    private CreateProductCommand command(Long taxId) {
        return new CreateProductCommand("Croqueta", "P-001", new BigDecimal("10.00"),
                new BigDecimal("15.00"), 100, 10, "Proveedor", true, false, "notas",
                3L, taxId, 5L);
    }

    @Test
    void creates_product_with_tax() {
        var service = new CreateProductService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        ProductDto dto = service.execute(command(7L));

        assertEquals(1L, dto.id());
        assertEquals("Croqueta", dto.name());
        assertEquals("P-001", dto.code());
        assertEquals(3L, dto.productCategory().id());
        assertNotNull(dto.tax());
        assertEquals(7L, dto.tax().id());
        assertEquals(5L, dto.company().id());
        assertTrue(dto.enabled());
    }

    @Test
    void creates_product_without_tax() {
        var service = new CreateProductService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.empty()));

        ProductDto dto = service.execute(command(null));

        assertEquals(1L, dto.id());
        assertNull(dto.tax());
        assertEquals(3L, dto.productCategory().id());
    }

    @Test
    void fails_when_company_not_found() {
        var service = new CreateProductService(repository,
                companyQueryPort(Optional.empty()),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L)));
    }

    @Test
    void fails_when_category_not_found() {
        var service = new CreateProductService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.empty()),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L)));
    }

    @Test
    void fails_when_tax_id_present_but_not_found() {
        var service = new CreateProductService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(99L)));
    }

    @Test
    void rejects_negative_purchase_price_in_domain() {
        var service = new CreateProductService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateProductCommand("Croqueta", "P-001", new BigDecimal("-1.00"),
                        new BigDecimal("15.00"), 100, 10, null, false, false, null,
                        3L, 7L, 5L)));
    }
}
