package com.vetsoftware.app.servicecategory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceChildrenQueryPort;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryHasActiveChildrenException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteServiceCategoryServiceTest {

    private final CompanyRef company = new CompanyRef(7L, "VetCo", "VC-001");

    private ServiceCategoryRepository repositoryWith(ServiceCategory existing, boolean[] deletedFlag) {
        return new ServiceCategoryRepository() {
            public ServiceCategory save(ServiceCategory s) { return s; }
            public Optional<ServiceCategory> findById(Long id) { return Optional.ofNullable(existing); }
            public List<ServiceCategory> findAllByCompanyId(Long companyId) { return List.of(); }
            public void delete(Long id) { deletedFlag[0] = true; }
            public int reactivate(Long id) { return 0; }
        };
    }

    @Test
    void deletes_when_no_active_children() {
        ServiceCategory existing = new ServiceCategory(1L, "Cat", "desc", company, LocalDateTime.now(), true);
        boolean[] deleted = {false};
        DeleteServiceCategoryService service = new DeleteServiceCategoryService(
                repositoryWith(existing, deleted), categoryId -> false);

        service.execute(1L);

        assertTrue(deleted[0]);
    }

    @Test
    void throws_not_found_when_missing() {
        boolean[] deleted = {false};
        DeleteServiceCategoryService service = new DeleteServiceCategoryService(
                repositoryWith(null, deleted), categoryId -> false);

        assertThrows(ServiceCategoryNotFoundException.class, () -> service.execute(1L));
        assertFalse(deleted[0]);
    }

    @Test
    void throws_when_has_active_children() {
        ServiceCategory existing = new ServiceCategory(1L, "Cat", "desc", company, LocalDateTime.now(), true);
        boolean[] deleted = {false};
        ServiceChildrenQueryPort childrenWithActive = categoryId -> true;
        DeleteServiceCategoryService service = new DeleteServiceCategoryService(
                repositoryWith(existing, deleted), childrenWithActive);

        assertThrows(ServiceCategoryHasActiveChildrenException.class, () -> service.execute(1L));
        assertFalse(deleted[0]);
    }
}
