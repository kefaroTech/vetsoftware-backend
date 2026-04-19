package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeleteCompanyServiceTest {
    private Company stored = new Company(1L, "VetClinic", "ID-001", null, null, LocalDateTime.now(), null);
    private boolean deleted;
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public Company save(Company c) { return c; }
        @Override public Optional<Company> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(Long id) { deleted = true; }
    };
    private final DeleteCompanyService service = new DeleteCompanyService(repository);

    @Test
    void delete_existing_company_removes_it() {
        service.execute(1L);
        assertTrue(deleted);
    }

    @Test
    void delete_non_existing_company_throws() {
        stored = null;
        assertThrows(CompanyNotFoundException.class, () -> service.execute(99L));
        assertFalse(deleted);
    }
}
