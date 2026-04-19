package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeleteCompanyServiceTest {
    private Company stored = Company.create("VetClinic", "ID-001", null, null, null);
    private boolean deleted;
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public void save(Company c) {}
        @Override public Optional<Company> findById(CompanyId id) { return Optional.ofNullable(stored); }
        @Override public List<Company> findAll() { return List.of(); }
        @Override public void delete(CompanyId id) { deleted = true; }
    };
    private final DeleteCompanyService service = new DeleteCompanyService(repository);

    @Test
    void delete_existing_company_removes_it() {
        service.execute(stored.getId());
        assertTrue(deleted);
    }

    @Test
    void delete_non_existing_company_throws() {
        stored = null;
        assertThrows(CompanyNotFoundException.class, () -> service.execute(CompanyId.of("non-existent")));
        assertFalse(deleted);
    }
}
