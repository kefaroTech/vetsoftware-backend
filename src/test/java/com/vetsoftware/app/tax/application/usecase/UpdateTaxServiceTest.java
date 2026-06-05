package com.vetsoftware.app.tax.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.tax.application.command.UpdateTaxCommand;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateTaxServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");

    private TaxRepository repositoryWith(Tax existing) {
        return new TaxRepository() {
            Tax stored = existing;
            @Override public Tax save(Tax tax) { stored = tax; return tax; }
            @Override public Optional<Tax> findById(Long id) { return Optional.ofNullable(stored); }
            @Override public List<Tax> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) {
        return companyId -> result;
    }

    @Test
    void updates_existing_tax() {
        Tax existing = new Tax(1L, "IVA", new BigDecimal("19.00"), company,
                LocalDateTime.now(), true);
        var service = new UpdateTaxService(repositoryWith(existing), companyQueryPort(Optional.of(company)));

        TaxDto dto = service.execute(new UpdateTaxCommand(1L, "IVA Reducido", new BigDecimal("5.00"), 5L));

        assertEquals("IVA Reducido", dto.name());
        assertEquals(new BigDecimal("5.00"), dto.percentage());
    }

    @Test
    void fails_when_tax_not_found() {
        var service = new UpdateTaxService(repositoryWith(null), companyQueryPort(Optional.of(company)));

        assertThrows(TaxNotFoundException.class,
                () -> service.execute(new UpdateTaxCommand(99L, "IVA", new BigDecimal("19.00"), 5L)));
    }

    @Test
    void fails_when_company_not_found() {
        Tax existing = new Tax(1L, "IVA", new BigDecimal("19.00"), company,
                LocalDateTime.now(), true);
        var service = new UpdateTaxService(repositoryWith(existing), companyQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new UpdateTaxCommand(1L, "IVA", new BigDecimal("19.00"), 99L)));
    }
}
