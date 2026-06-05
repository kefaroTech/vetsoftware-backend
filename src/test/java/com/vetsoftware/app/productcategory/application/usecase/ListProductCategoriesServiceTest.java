package com.vetsoftware.app.productcategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListProductCategoriesServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "Acme Vet", "ACME-001");

    private Long queriedCompanyId;

    private final ProductCategoryRepository repository = new ProductCategoryRepository() {
        public ProductCategory save(ProductCategory pc) { return pc; }
        public Optional<ProductCategory> findById(Long id) { return Optional.empty(); }
        public List<ProductCategory> findAll() { return List.of(); }
        public List<ProductCategory> findAllByCompanyId(Long companyId) {
            queriedCompanyId = companyId;
            return List.of(
                    new ProductCategory(1L, "Food", "desc1", company, LocalDateTime.now(), true),
                    new ProductCategory(2L, "Toys", "desc2", company, LocalDateTime.now(), true));
        }
        public void delete(Long id) {}
        public int reactivate(Long id) { return 0; }
    };

    @Test
    void lists_categories_scoped_to_company() {
        ListProductCategoriesService service = new ListProductCategoriesService(repository);

        List<ProductCategoryDto> result = service.listByCompany(7L);

        assertEquals(7L, queriedCompanyId);
        assertEquals(2, result.size());
        assertEquals("Food", result.get(0).name());
        assertEquals("Toys", result.get(1).name());
    }
}
