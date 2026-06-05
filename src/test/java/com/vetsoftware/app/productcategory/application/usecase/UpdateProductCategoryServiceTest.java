package com.vetsoftware.app.productcategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vetsoftware.app.productcategory.application.command.UpdateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateProductCategoryServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "Acme Vet", "ACME-001");

    private ProductCategory stored;
    private ProductCategory saved;

    private final ProductCategoryRepository repository = new ProductCategoryRepository() {
        public ProductCategory save(ProductCategory pc) { saved = pc; return pc; }
        public Optional<ProductCategory> findById(Long id) { return Optional.ofNullable(stored); }
        public List<ProductCategory> findAll() { return List.of(); }
        public List<ProductCategory> findAllByCompanyId(Long companyId) { return List.of(); }
        public void delete(Long id) {}
        public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) {
        return companyId -> result;
    }

    @Test
    void updates_existing_product_category() {
        stored = new ProductCategory(5L, "Old", "Old desc", company, LocalDateTime.now(), true);
        UpdateProductCategoryService service =
                new UpdateProductCategoryService(repository, companyQueryPort(Optional.of(company)));

        ProductCategoryDto dto = service.execute(
                new UpdateProductCategoryCommand(5L, "New", "New desc", 7L));

        assertEquals("New", dto.name());
        assertEquals("New desc", dto.description());
        assertEquals("New", saved.getName());
    }

    @Test
    void throws_not_found_when_missing() {
        stored = null;
        UpdateProductCategoryService service =
                new UpdateProductCategoryService(repository, companyQueryPort(Optional.of(company)));

        assertThrows(ProductCategoryNotFoundException.class, () -> service.execute(
                new UpdateProductCategoryCommand(404L, "New", "New desc", 7L)));
    }

    @Test
    void throws_when_company_not_found() {
        stored = new ProductCategory(5L, "Old", "Old desc", company, LocalDateTime.now(), true);
        UpdateProductCategoryService service =
                new UpdateProductCategoryService(repository, companyQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateProductCategoryCommand(5L, "New", "New desc", 404L)));
    }
}
