package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.CreateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateCompanyServiceTest {
    private Company saved;
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public void save(Company c) { saved = c; }
        @Override public Optional<Company> findById(CompanyId id) { return Optional.ofNullable(saved); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(CompanyId id) {}
    };
    private final CreateCompanyService service = new CreateCompanyService(repository);

    @Test
    void create_company_saves_and_returns_dto() {
        CreateCompanyCommand command = new CreateCompanyCommand("VetClinic", "ID-001", "123 Main St", "555-1234", null);
        CompanyDto dto = service.execute(command);
        assertNotNull(dto.id());
        assertEquals("VetClinic", dto.name());
        assertEquals("ID-001", dto.identifier());
        assertNotNull(dto.createdDate());
        assertNotNull(saved);
    }

    @Test
    void create_company_with_blank_name_throws() {
        CreateCompanyCommand command = new CreateCompanyCommand("", "ID-001", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.execute(command));
    }

    @Test
    void create_company_with_blank_identifier_throws() {
        CreateCompanyCommand command = new CreateCompanyCommand("VetClinic", "  ", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.execute(command));
    }
}
