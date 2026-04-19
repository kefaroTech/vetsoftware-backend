package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateCompanyServiceTest {
    private Company preset = new Company(1L, "OldName", "OLD-001", "Old Address", "555-0000", java.time.LocalDateTime.now(), null);
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public Company save(Company c) { return c; }
        @Override public Optional<Company> findById(Long id) { return Optional.ofNullable(preset); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final UpdateCompanyService service = new UpdateCompanyService(repository);

    @Test
    void update_company_changes_fields_and_returns_dto() {
        CompanyDto dto = service.execute(new UpdateCompanyCommand(1L, "NewName", "NEW-001", "New Address", "555-9999"));
        assertEquals("NewName", dto.name());
        assertEquals("NEW-001", dto.identifier());
    }

    @Test
    void update_company_not_found_throws() {
        preset = null;
        assertThrows(CompanyNotFoundException.class, () ->
            service.execute(new UpdateCompanyCommand(99L, "Name", "ID-001", null, null)));
    }
}
