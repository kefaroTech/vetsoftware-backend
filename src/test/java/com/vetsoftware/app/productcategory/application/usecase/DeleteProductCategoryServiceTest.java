package com.vetsoftware.app.productcategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.application.port.out.ProductChildrenQueryPort;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryHasActiveChildrenException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteProductCategoryServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "Acme Vet", "ACME-001");

    private ProductCategory stored;
    private Long deletedId;

    private final ProductCategoryRepository repository = new ProductCategoryRepository() {
        public ProductCategory save(ProductCategory pc) { return pc; }
        public Optional<ProductCategory> findById(Long id) { return Optional.ofNullable(stored); }
        public List<ProductCategory> findAll() { return List.of(); }
        public List<ProductCategory> findAllByCompanyId(Long companyId) { return List.of(); }
        public void delete(Long id) { deletedId = id; }
        public int reactivate(Long id) { return 0; }
    };

    private ProductChildrenQueryPort childrenPort(boolean hasChildren) {
        return categoryId -> hasChildren;
    }

    @Test
    void deletes_when_no_active_children() {
        stored = new ProductCategory(5L, "Food", "desc", company, LocalDateTime.now(), true);
        DeleteProductCategoryService service =
                new DeleteProductCategoryService(repository, childrenPort(false));

        service.execute(5L);

        assertEquals(5L, deletedId);
    }

    @Test
    void throws_not_found_when_missing() {
        stored = null;
        DeleteProductCategoryService service =
                new DeleteProductCategoryService(repository, childrenPort(false));

        assertThrows(ProductCategoryNotFoundException.class, () -> service.execute(404L));
    }

    @Test
    void throws_has_active_children_when_products_exist() {
        stored = new ProductCategory(5L, "Food", "desc", company, LocalDateTime.now(), true);
        DeleteProductCategoryService service =
                new DeleteProductCategoryService(repository, childrenPort(true));

        ProductCategoryHasActiveChildrenException ex = assertThrows(
                ProductCategoryHasActiveChildrenException.class, () -> service.execute(5L));
        assertTrue(ex.getMessage().contains("product"));
    }
}
