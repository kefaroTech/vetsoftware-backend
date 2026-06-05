package com.vetsoftware.app.service.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.service.application.command.CreateServiceCommand;
import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceCategoryQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.TaxQueryPort;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateServiceServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ServiceCategoryRef category = new ServiceCategoryRef(3L, "Consultas");
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));

    private Service savedService;

    private final ServiceRepository repository = new ServiceRepository() {
        @Override public Service save(Service service) {
            savedService = new Service(1L, service.getName(), service.getPrice(),
                    service.isHasTax(), service.getNotes(), service.getServiceCategory(),
                    service.getTax(), service.getCompany(), service.getCreatedDate(), service.isEnabled());
            return savedService;
        }
        @Override public Optional<Service> findById(Long id) { return Optional.ofNullable(savedService); }
        @Override public List<Service> findAll() { return List.of(); }
        @Override public List<Service> findAllByCompanyId(Long companyId) { return List.of(); }
        @Override public PageResult<Service> search(SearchServicesCommand command) {
            return new PageResult<>(List.of(), 0, 20, 0, 0);
        }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) { return companyId -> result; }
    private ServiceCategoryQueryPort categoryQueryPort(Optional<ServiceCategoryRef> result) { return id -> result; }
    private TaxQueryPort taxQueryPort(Optional<TaxRef> result) { return id -> result; }

    private CreateServiceCommand command(Long taxId) {
        return new CreateServiceCommand("Consulta general", new BigDecimal("50.00"), true, "notas",
                3L, taxId, 5L);
    }

    @Test
    void creates_service_with_tax() {
        var service = new CreateServiceService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        ServiceDto dto = service.execute(command(7L));

        assertEquals(1L, dto.id());
        assertEquals("Consulta general", dto.name());
        assertEquals(3L, dto.serviceCategory().id());
        assertNotNull(dto.tax());
        assertEquals(7L, dto.tax().id());
        assertEquals(5L, dto.company().id());
        assertTrue(dto.enabled());
    }

    @Test
    void creates_service_without_tax() {
        var service = new CreateServiceService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.empty()));

        ServiceDto dto = service.execute(command(null));

        assertEquals(1L, dto.id());
        assertNull(dto.tax());
        assertEquals(3L, dto.serviceCategory().id());
    }

    @Test
    void fails_when_company_not_found() {
        var service = new CreateServiceService(repository,
                companyQueryPort(Optional.empty()),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L)));
    }

    @Test
    void fails_when_category_not_found() {
        var service = new CreateServiceService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.empty()),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L)));
    }

    @Test
    void fails_when_tax_id_present_but_not_found() {
        var service = new CreateServiceService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(99L)));
    }

    @Test
    void rejects_negative_price_in_domain() {
        var service = new CreateServiceService(repository,
                companyQueryPort(Optional.of(company)),
                categoryQueryPort(Optional.of(category)),
                taxQueryPort(Optional.of(tax)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateServiceCommand("Consulta", new BigDecimal("-1.00"), false, null,
                        3L, 7L, 5L)));
    }
}
