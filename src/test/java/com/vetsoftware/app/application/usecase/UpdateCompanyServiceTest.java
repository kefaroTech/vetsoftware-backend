package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateCompanyServiceTest {
    private Company preset = Company.create("OldName", "OLD-001", "Old Address", "555-0000", null);
    private Company saved;
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public void save(Company c) { saved = c; }
        @Override public Optional<Company> findById(CompanyId id) { return Optional.ofNullable(preset); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(CompanyId id) {}
    };
    private final UpdateCompanyService service = new UpdateCompanyService(repository);

    @Test
    void update_company_changes_fields_and_returns_dto() {
        UpdateCompanyCommand command = new UpdateCompanyCommand(preset.getId().value(), "NewName", "NEW-001", "New Address", "555-9999");
        CompanyDto dto = service.execute(command);
        assertEquals("NewName", dto.name());
        assertEquals("NEW-001", dto.identifier());
        assertNotNull(saved);
    }

    @Test
    void update_company_not_found_throws() {
        preset = null;
        UpdateCompanyCommand command = new UpdateCompanyCommand("non-existent-id", "Name", "ID-001", null, null);
        assertThrows(CompanyNotFoundException.class, () -> service.execute(command));
    }
}
