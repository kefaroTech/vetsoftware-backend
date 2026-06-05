package com.vetsoftware.app.tax.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.tax.application.command.CreateTaxCommand;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateTaxServiceTest {

    private Tax savedTax;

    private final TaxRepository repository = new TaxRepository() {
        @Override public Tax save(Tax tax) {
            savedTax = new Tax(1L, tax.getName(), tax.getPercentage(), tax.getCompany(),
                    tax.getCreatedDate(), tax.isEnabled());
            return savedTax;
        }
        @Override public Optional<Tax> findById(Long id) { return Optional.ofNullable(savedTax); }
        @Override public List<Tax> findAllByCompanyId(Long companyId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) {
        return companyId -> result;
    }

    @Test
    void creates_tax_loading_company_ref() {
        var service = new CreateTaxService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))));

        TaxDto dto = service.execute(new CreateTaxCommand("IVA", new BigDecimal("19.00"), 5L));

        assertEquals(1L, dto.id());
        assertEquals("IVA", dto.name());
        assertEquals(new BigDecimal("19.00"), dto.percentage());
        assertEquals(5L, dto.company().id());
        assertTrue(dto.enabled());
    }

    @Test
    void fails_when_company_not_found() {
        var service = new CreateTaxService(repository, companyQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new CreateTaxCommand("IVA", new BigDecimal("19.00"), 99L)));
    }

    @Test
    void rejects_negative_percentage_in_domain() {
        var service = new CreateTaxService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new CreateTaxCommand("IVA", new BigDecimal("-1.00"), 5L)));
    }
}
