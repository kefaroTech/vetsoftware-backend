package com.vetsoftware.app.service.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.command.UpdateServiceCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceCategoryQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.TaxQueryPort;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import com.vetsoftware.app.service.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateServiceServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ServiceCategoryRef category = new ServiceCategoryRef(3L, "Consultas");
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));

    private Service existing() {
        return new Service(1L, "Consulta general", new BigDecimal("50.00"), true, "notas",
                category, tax, company, LocalDateTime.now(), true);
    }

    private ServiceRepository repositoryWith(Service stored) {
        return new ServiceRepository() {
            Service current = stored;
            @Override public Service save(Service service) { current = service; return service; }
            @Override public Optional<Service> findById(Long id) { return Optional.ofNullable(current); }
            @Override public List<Service> findAll() { return List.of(); }
            @Override public List<Service> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public PageResult<Service> search(SearchServicesCommand command) {
                return new PageResult<>(List.of(), 0, 20, 0, 0);
            }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) { return companyId -> result; }
    private ServiceCategoryQueryPort categoryQueryPort(Optional<ServiceCategoryRef> result) { return id -> result; }
    private TaxQueryPort taxQueryPort(Optional<TaxRef> result) { return id -> result; }

    private UpdateServiceCommand command(Long taxId) {
        return new UpdateServiceCommand(1L, "Consulta especializada", new BigDecimal("80.00"), false,
                "actualizado", 3L, taxId, 5L);
    }

    @Test
    void updates_existing_service() {
        var service = new UpdateServiceService(repositoryWith(existing()),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        ServiceDto dto = service.execute(command(7L));

        assertEquals("Consulta especializada", dto.name());
        assertEquals(new BigDecimal("80.00"), dto.price());
        assertEquals(7L, dto.tax().id());
    }

    @Test
    void updates_clearing_tax() {
        var service = new UpdateServiceService(repositoryWith(existing()),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.empty()));

        ServiceDto dto = service.execute(command(null));

        assertNull(dto.tax());
    }

    @Test
    void fails_when_service_not_found() {
        var service = new UpdateServiceService(repositoryWith(null),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        assertThrows(ServiceNotFoundException.class, () -> service.execute(command(7L)));
    }

    @Test
    void fails_when_category_not_found() {
        var service = new UpdateServiceService(repositoryWith(existing()),
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.empty()),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L)));
    }
}
