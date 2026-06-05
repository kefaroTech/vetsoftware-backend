package com.vetsoftware.app.servicecategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListServiceCategoriesServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "VetCo", "VC-001");

    @Test
    void lists_categories_for_company() {
        ServiceCategoryRepository repository = new ServiceCategoryRepository() {
            public ServiceCategory save(ServiceCategory s) { return s; }
            public Optional<ServiceCategory> findById(Long id) { return Optional.empty(); }
            public List<ServiceCategory> findAllByCompanyId(Long companyId) {
                return List.of(
                        new ServiceCategory(1L, "A", "desc A", company, LocalDateTime.now(), true),
                        new ServiceCategory(2L, "B", "desc B", company, LocalDateTime.now(), true));
            }
            public void delete(Long id) {}
            public int reactivate(Long id) { return 0; }
        };
        ListServiceCategoriesService service = new ListServiceCategoriesService(repository);

        List<ServiceCategoryDto> result = service.listByCompany(7L);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).name());
        assertEquals(7L, result.get(0).company().id());
    }

    @Test
    void returns_empty_when_no_categories() {
        ServiceCategoryRepository repository = new ServiceCategoryRepository() {
            public ServiceCategory save(ServiceCategory s) { return s; }
            public Optional<ServiceCategory> findById(Long id) { return Optional.empty(); }
            public List<ServiceCategory> findAllByCompanyId(Long companyId) { return List.of(); }
            public void delete(Long id) {}
            public int reactivate(Long id) { return 0; }
        };
        ListServiceCategoriesService service = new ListServiceCategoriesService(repository);

        assertEquals(0, service.listByCompany(7L).size());
    }
}
