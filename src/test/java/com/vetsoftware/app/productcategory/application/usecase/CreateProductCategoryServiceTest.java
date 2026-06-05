package com.vetsoftware.app.productcategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vetsoftware.app.productcategory.application.command.CreateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateProductCategoryServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "Acme Vet", "ACME-001");

    private ProductCategory saved;

    private final ProductCategoryRepository repository = new ProductCategoryRepository() {
        public ProductCategory save(ProductCategory pc) {
            saved = new ProductCategory(99L, pc.getName(), pc.getDescription(), pc.getCompany(),
                    pc.getCreatedDate(), pc.isEnabled());
            return saved;
        }
        public Optional<ProductCategory> findById(Long id) { return Optional.ofNullable(saved); }
        public List<ProductCategory> findAll() { return List.of(); }
        public List<ProductCategory> findAllByCompanyId(Long companyId) { return List.of(); }
        public void delete(Long id) {}
        public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) {
        return companyId -> result;
    }

    @Test
    void creates_product_category_with_company_from_command() {
        CreateProductCategoryService service =
                new CreateProductCategoryService(repository, companyQueryPort(Optional.of(company)));

        ProductCategoryDto dto = service.execute(
                new CreateProductCategoryCommand("Food", "Pet food category", 7L));

        assertNotNull(dto);
        assertEquals(99L, dto.id());
        assertEquals("Food", dto.name());
        assertEquals("Pet food category", dto.description());
        assertEquals(7L, dto.company().id());
        assertEquals(true, dto.enabled());
    }

    @Test
    void throws_when_company_not_found() {
        CreateProductCategoryService service =
                new CreateProductCategoryService(repository, companyQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateProductCategoryCommand("Food", "Pet food category", 404L)));
    }

    @Test
    void throws_when_name_blank() {
        CreateProductCategoryService service =
                new CreateProductCategoryService(repository, companyQueryPort(Optional.of(company)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateProductCategoryCommand("  ", "Pet food category", 7L)));
    }
}
