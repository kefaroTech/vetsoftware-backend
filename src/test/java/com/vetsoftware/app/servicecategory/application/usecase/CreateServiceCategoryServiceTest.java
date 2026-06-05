package com.vetsoftware.app.servicecategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.servicecategory.application.command.CreateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateServiceCategoryServiceTest {

    private final ServiceCategoryRepository repository = new ServiceCategoryRepository() {
        ServiceCategory saved;
        public ServiceCategory save(ServiceCategory s) { saved = new ServiceCategory(1L, s.getName(), s.getDescription(), s.getCompany(), s.getCreatedDate(), s.isEnabled()); return saved; }
        public Optional<ServiceCategory> findById(Long id) { return Optional.ofNullable(saved); }
        public List<ServiceCategory> findAllByCompanyId(Long companyId) { return List.of(); }
        public void delete(Long id) {}
        public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyWith(CompanyRef ref) {
        return companyId -> Optional.ofNullable(ref);
    }

    @Test
    void creates_service_category_with_company() {
        CompanyRef company = new CompanyRef(7L, "VetCo", "VC-001");
        CreateServiceCategoryService service = new CreateServiceCategoryService(repository, companyWith(company));

        ServiceCategoryDto dto = service.execute(
                new CreateServiceCategoryCommand("Grooming", "Grooming services", 7L));

        assertEquals(1L, dto.id());
        assertEquals("Grooming", dto.name());
        assertEquals("Grooming services", dto.description());
        assertEquals(7L, dto.company().id());
        assertTrue(dto.enabled());
    }

    @Test
    void throws_when_company_not_found() {
        CreateServiceCategoryService service = new CreateServiceCategoryService(repository, companyWith(null));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateServiceCategoryCommand("Grooming", "desc", 99L)));
    }

    @Test
    void throws_when_name_blank() {
        CompanyRef company = new CompanyRef(7L, "VetCo", "VC-001");
        CreateServiceCategoryService service = new CreateServiceCategoryService(repository, companyWith(company));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateServiceCategoryCommand("  ", "desc", 7L)));
    }
}
