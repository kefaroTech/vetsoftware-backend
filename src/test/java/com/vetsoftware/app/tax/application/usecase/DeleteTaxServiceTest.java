package com.vetsoftware.app.tax.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.tax.application.port.out.TaxChildrenQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxHasActiveChildrenException;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteTaxServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");

    private TaxRepository repositoryWith(Tax existing, boolean[] deleted) {
        return new TaxRepository() {
            @Override public Tax save(Tax tax) { return tax; }
            @Override public Optional<Tax> findById(Long id) { return Optional.ofNullable(existing); }
            @Override public List<Tax> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public void delete(Long id) { deleted[0] = true; }
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private TaxChildrenQueryPort childrenPort(boolean hasChildren) {
        return taxId -> hasChildren;
    }

    @Test
    void deletes_when_no_active_children() {
        Tax existing = new Tax(1L, "IVA", new BigDecimal("19.00"), company, LocalDateTime.now(), true);
        boolean[] deleted = {false};
        var service = new DeleteTaxService(repositoryWith(existing, deleted), childrenPort(false));

        service.execute(1L);

        assertTrue(deleted[0]);
    }

    @Test
    void fails_when_tax_not_found() {
        boolean[] deleted = {false};
        var service = new DeleteTaxService(repositoryWith(null, deleted), childrenPort(false));

        assertThrows(TaxNotFoundException.class, () -> service.execute(99L));
        assertFalse(deleted[0]);
    }

    @Test
    void fails_when_has_active_children() {
        Tax existing = new Tax(1L, "IVA", new BigDecimal("19.00"), company, LocalDateTime.now(), true);
        boolean[] deleted = {false};
        var service = new DeleteTaxService(repositoryWith(existing, deleted), childrenPort(true));

        assertThrows(TaxHasActiveChildrenException.class, () -> service.execute(1L));
        assertFalse(deleted[0]);
    }
}
