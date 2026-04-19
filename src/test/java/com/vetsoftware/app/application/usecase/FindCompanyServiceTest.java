package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindCompanyServiceTest {
    private Company stored = new Company(1L, "VetClinic", "ID-001", "123 Main St", "555-1234", java.time.LocalDateTime.now(), null);
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public Company save(Company c) { return c; }
        @Override public Optional<Company> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final FindCompanyService service = new FindCompanyService(repository);

    @Test
    void find_existing_company_returns_dto() {
        CompanyDto dto = service.findById(1L);
        assertEquals("VetClinic", dto.name());
        assertEquals("ID-001", dto.identifier());
    }

    @Test
    void find_non_existing_company_throws() {
        stored = null;
        assertThrows(CompanyNotFoundException.class, () -> service.findById(99L));
    }
}
