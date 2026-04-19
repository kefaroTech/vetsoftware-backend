package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.CreateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateCompanyServiceTest {
    private final AtomicLong sequence = new AtomicLong(1);
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public Company save(Company c) { return new Company(sequence.getAndIncrement(), c.getName(), c.getIdentifier(), c.getAddress(), c.getContactNumber(), c.getCreatedDate(), c.getCreatedBy()); }
        @Override public Optional<Company> findById(Long id) { return Optional.empty(); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
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
    }

    @Test
    void create_company_with_blank_name_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateCompanyCommand("", "ID-001", null, null, null)));
    }

    @Test
    void create_company_with_blank_identifier_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateCompanyCommand("VetClinic", "  ", null, null, null)));
    }
}
