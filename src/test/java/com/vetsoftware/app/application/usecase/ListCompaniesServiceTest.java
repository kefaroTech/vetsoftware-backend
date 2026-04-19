package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListCompaniesServiceTest {
    private final List<Company> companies = List.of(
        Company.create("VetClinic A", "ID-001", null, null, null),
        Company.create("VetClinic B", "ID-002", null, null, null)
    );
    private final CompanyRepository repository = new CompanyRepository() {
        @Override public void save(Company c) {}
        @Override public Optional<Company> findById(CompanyId id) { return Optional.empty(); }
        @Override public List<Company> findAll() { return companies; }
        @Override public void delete(CompanyId id) {}
    };
    private final ListCompaniesService service = new ListCompaniesService(repository);

    @Test
    void list_all_returns_all_companies() {
        List<CompanyDto> result = service.listAll();
        assertEquals(2, result.size());
        assertEquals("VetClinic A", result.get(0).name());
        assertEquals("VetClinic B", result.get(1).name());
    }

    @Test
    void list_all_empty_returns_empty_list() {
        CompanyRepository emptyRepo = new CompanyRepository() {
            @Override public void save(Company c) {}
            @Override public Optional<Company> findById(CompanyId id) { return Optional.empty(); }
            @Override public List<Company> findAll() { return List.of(); }
            @Override public void delete(CompanyId id) {}
        };
        assertTrue(new ListCompaniesService(emptyRepo).listAll().isEmpty());
    }
}
