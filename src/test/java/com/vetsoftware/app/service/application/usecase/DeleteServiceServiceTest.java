package com.vetsoftware.app.service.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteServiceServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ServiceCategoryRef category = new ServiceCategoryRef(3L, "Consultas");

    private Service existing() {
        return new Service(1L, "Consulta general", new BigDecimal("50.00"), false, null,
                category, null, company, LocalDateTime.now(), true);
    }

    private ServiceRepository repositoryWith(Service existing, boolean[] deleted) {
        return new ServiceRepository() {
            @Override public Service save(Service service) { return service; }
            @Override public Optional<Service> findById(Long id) { return Optional.ofNullable(existing); }
            @Override public List<Service> findAll() { return List.of(); }
            @Override public List<Service> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public PageResult<Service> search(SearchServicesCommand command) {
                return new PageResult<>(List.of(), 0, 20, 0, 0);
            }
            @Override public void delete(Long id) { deleted[0] = true; }
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    @Test
    void deletes_existing_service() {
        boolean[] deleted = {false};
        var service = new DeleteServiceService(repositoryWith(existing(), deleted));

        service.execute(1L);

        assertTrue(deleted[0]);
    }

    @Test
    void fails_when_service_not_found() {
        boolean[] deleted = {false};
        var service = new DeleteServiceService(repositoryWith(null, deleted));

        assertThrows(ServiceNotFoundException.class, () -> service.execute(99L));
        assertFalse(deleted[0]);
    }
}
