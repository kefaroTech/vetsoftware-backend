package com.vetsoftware.app.service.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchServicesServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final ServiceCategoryRef category = new ServiceCategoryRef(3L, "Consultas");
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));

    private Service service() {
        return new Service(1L, "Consulta general", new BigDecimal("50.00"), true, "notas",
                category, tax, company, LocalDateTime.now(), true);
    }

    private ServiceRepository repositoryReturning(PageResult<Service> page, SearchServicesCommand[] captured) {
        return new ServiceRepository() {
            @Override public Service save(Service service) { return service; }
            @Override public Optional<Service> findById(Long id) { return Optional.empty(); }
            @Override public List<Service> findAll() { return List.of(); }
            @Override public List<Service> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public PageResult<Service> search(SearchServicesCommand command) {
                captured[0] = command;
                return page;
            }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    @Test
    void maps_page_result_to_dtos() {
        var page = new PageResult<>(List.of(service()), 0, 20, 1, 1);
        SearchServicesCommand[] captured = {null};
        var search = new SearchServicesService(repositoryReturning(page, captured));

        PageResult<ServiceDto> result = search.execute(
                new SearchServicesCommand(5L, "Cons", 3L, 7L, 0, 20));

        assertEquals(1, result.content().size());
        ServiceDto dto = result.content().get(0);
        assertEquals("Consulta general", dto.name());
        assertEquals(3L, dto.serviceCategory().id());
        assertEquals(7L, dto.tax().id());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void forwards_command_to_repository() {
        var page = new PageResult<Service>(List.of(), 2, 5, 0, 0);
        SearchServicesCommand[] captured = {null};
        var search = new SearchServicesService(repositoryReturning(page, captured));

        var command = new SearchServicesCommand(5L, null, 3L, null, 2, 5);
        search.execute(command);

        assertSame(command, captured[0]);
        assertEquals(5L, captured[0].companyId());
        assertEquals(2, captured[0].page());
        assertEquals(5, captured[0].pageSize());
    }

    @Test
    void returns_empty_page_when_no_matches() {
        var page = new PageResult<Service>(List.of(), 0, 20, 0, 0);
        SearchServicesCommand[] captured = {null};
        var search = new SearchServicesService(repositoryReturning(page, captured));

        PageResult<ServiceDto> result = search.execute(
                new SearchServicesCommand(5L, "nada", null, null, 0, 20));

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }
}
