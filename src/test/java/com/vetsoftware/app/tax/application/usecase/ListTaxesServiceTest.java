package com.vetsoftware.app.tax.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListTaxesServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");

    @Test
    void lists_taxes_of_given_company() {
        Long[] requestedCompanyId = {null};
        TaxRepository repository = new TaxRepository() {
            @Override public Tax save(Tax tax) { return tax; }
            @Override public Optional<Tax> findById(Long id) { return Optional.empty(); }
            @Override public List<Tax> findAllByCompanyId(Long companyId) {
                requestedCompanyId[0] = companyId;
                return List.of(
                        new Tax(1L, "IVA", new BigDecimal("19.00"), company, LocalDateTime.now(), true),
                        new Tax(2L, "INC", new BigDecimal("8.00"), company, LocalDateTime.now(), true));
            }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
        var service = new ListTaxesService(repository);

        List<TaxDto> result = service.listByCompany(5L);

        assertEquals(5L, requestedCompanyId[0]);
        assertEquals(2, result.size());
        assertEquals("IVA", result.get(0).name());
        assertEquals("INC", result.get(1).name());
    }

    @Test
    void returns_empty_list_when_no_taxes() {
        TaxRepository repository = new TaxRepository() {
            @Override public Tax save(Tax tax) { return tax; }
            @Override public Optional<Tax> findById(Long id) { return Optional.empty(); }
            @Override public List<Tax> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
        var service = new ListTaxesService(repository);

        assertTrue(service.listByCompany(5L).isEmpty());
    }
}
