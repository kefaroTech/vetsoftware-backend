package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindCompanyServiceTest {
    private Company stored = Company.create("VetClinic", "ID-001", "123 Main St", "555-1234", null);
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public void save(Company c) {}
        @Override public Optional<Company> findById(CompanyId id) { return Optional.ofNullable(stored); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(CompanyId id) {}
    };
    private final FindCompanyService service = new FindCompanyService(repository);

    @Test
    void find_existing_company_returns_dto() {
        CompanyDto dto = service.findById(stored.getId());
        assertEquals("VetClinic", dto.name());
        assertEquals("ID-001", dto.identifier());
    }

    @Test
    void find_non_existing_company_throws() {
        stored = null;
        assertThrows(CompanyNotFoundException.class, () -> service.findById(CompanyId.of("non-existent")));
    }
}
