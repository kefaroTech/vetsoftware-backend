package com.vetsoftware.app.servicecategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateServiceCategoryServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "VetCo", "VC-001");

    private ServiceCategoryRepository repositoryWith(ServiceCategory existing) {
        return new ServiceCategoryRepository() {
            ServiceCategory current = existing;
            public ServiceCategory save(ServiceCategory s) { current = s; return s; }
            public Optional<ServiceCategory> findById(Long id) { return Optional.ofNullable(current); }
            public List<ServiceCategory> findAllByCompanyId(Long companyId) { return List.of(); }
            public void delete(Long id) {}
            public int reactivate(Long id) { return 0; }
        };
    }

    private CompanyQueryPort companyPort = companyId -> Optional.of(company);

    @Test
    void updates_existing_service_category() {
        ServiceCategory existing = new ServiceCategory(1L, "Old", "Old desc", company, LocalDateTime.now(), true);
        UpdateServiceCategoryService service = new UpdateServiceCategoryService(repositoryWith(existing), companyPort);

        ServiceCategoryDto dto = service.execute(
                new UpdateServiceCategoryCommand(1L, "New", "New desc", 7L));

        assertEquals("New", dto.name());
        assertEquals("New desc", dto.description());
    }

    @Test
    void throws_when_not_found() {
        UpdateServiceCategoryService service = new UpdateServiceCategoryService(repositoryWith(null), companyPort);

        assertThrows(ServiceCategoryNotFoundException.class, () -> service.execute(
                new UpdateServiceCategoryCommand(1L, "New", "New desc", 7L)));
    }

    @Test
    void throws_when_company_not_found() {
        ServiceCategory existing = new ServiceCategory(1L, "Old", "Old desc", company, LocalDateTime.now(), true);
        UpdateServiceCategoryService service = new UpdateServiceCategoryService(
                repositoryWith(existing), companyId -> Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateServiceCategoryCommand(1L, "New", "New desc", 99L)));
    }
}
