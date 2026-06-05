package com.vetsoftware.app.product.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchProductsServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ProductCategoryRef category = new ProductCategoryRef(3L, "Alimentos");
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));

    private Product product() {
        return new Product(1L, "Croqueta", "P-001", new BigDecimal("10.00"), new BigDecimal("15.00"),
                100, 10, "Proveedor", true, false, "notas", category, tax, company,
                LocalDateTime.now(), true);
    }

    private ProductRepository repositoryReturning(PageResult<Product> page, SearchProductsCommand[] captured) {
        return new ProductRepository() {
            @Override public Product save(Product product) { return product; }
            @Override public Optional<Product> findById(Long id) { return Optional.empty(); }
            @Override public List<Product> findAll() { return List.of(); }
            @Override public List<Product> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public PageResult<Product> search(SearchProductsCommand command) {
                captured[0] = command;
                return page;
            }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    @Test
    void maps_page_result_to_dtos() {
        var page = new PageResult<>(List.of(product()), 0, 20, 1, 1);
        SearchProductsCommand[] captured = {null};
        var service = new SearchProductsService(repositoryReturning(page, captured));

        PageResult<ProductDto> result = service.execute(
                new SearchProductsCommand(5L, "Croq", null, 3L, 7L, 0, 20));

        assertEquals(1, result.content().size());
        ProductDto dto = result.content().get(0);
        assertEquals("Croqueta", dto.name());
        assertEquals(3L, dto.productCategory().id());
        assertEquals(7L, dto.tax().id());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void forwards_command_to_repository() {
        var page = new PageResult<Product>(List.of(), 2, 5, 0, 0);
        SearchProductsCommand[] captured = {null};
        var service = new SearchProductsService(repositoryReturning(page, captured));

        var command = new SearchProductsCommand(5L, null, "P-0", 3L, null, 2, 5);
        service.execute(command);

        assertSame(command, captured[0]);
        assertEquals(5L, captured[0].companyId());
        assertEquals(2, captured[0].page());
        assertEquals(5, captured[0].pageSize());
    }

    @Test
    void returns_empty_page_when_no_matches() {
        var page = new PageResult<Product>(List.of(), 0, 20, 0, 0);
        SearchProductsCommand[] captured = {null};
        var service = new SearchProductsService(repositoryReturning(page, captured));

        PageResult<ProductDto> result = service.execute(
                new SearchProductsCommand(5L, "nada", null, null, null, 0, 20));

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }
}
