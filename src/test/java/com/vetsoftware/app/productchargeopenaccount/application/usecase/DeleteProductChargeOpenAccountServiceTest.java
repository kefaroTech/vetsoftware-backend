package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteProductChargeOpenAccountServiceTest {

    private final AnimalRef animal = new AnimalRef(2L, "Rex", "A-2");
    private final ProductRef product = new ProductRef(3L, "Vaccine", "P-3", new BigDecimal("20.00"));
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Dr. House");

    private final ProductChargeOpenAccount existing = new ProductChargeOpenAccount(1L, animal, product,
            new BigDecimal("20.00"), new OpenAccountRef(10L, 5L), createdBy, LocalDateTime.now(), true);

    private final List<Long> deleted = new ArrayList<>();
    private final List<Long> refreshed = new ArrayList<>();

    private final ProductChargeOpenAccountRepository repository = new ProductChargeOpenAccountRepository() {
        @Override public ProductChargeOpenAccount save(ProductChargeOpenAccount charge) { return charge; }
        @Override public Optional<ProductChargeOpenAccount> findById(Long id) {
            return id == 1L ? Optional.of(existing) : Optional.empty();
        }
        @Override public List<ProductChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) { deleted.add(id); }
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = refreshed::add;

    // Totales POST soft-delete (Hibernate ya filtró el cargo dado de baja).
    private OpenAccountTotalsQueryPort totals(String remainingCharges, String payments) {
        return new OpenAccountTotalsQueryPort() {
            @Override public BigDecimal totalCharges(Long openAccountId) { return new BigDecimal(remainingCharges); }
            @Override public BigDecimal totalPayments(Long openAccountId) { return new BigDecimal(payments); }
        };
    }

    @Test
    void deletes_and_refreshes_when_payments_within_remaining_charges() {
        var service = new DeleteProductChargeOpenAccountService(repository, totals("80.00", "50.00"), refresher);

        service.execute(1L);

        assertEquals(List.of(1L), deleted);
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void allows_delete_when_payments_equal_remaining_charges() {
        var service = new DeleteProductChargeOpenAccountService(repository, totals("50.00", "50.00"), refresher);

        service.execute(1L);

        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void rejects_delete_when_payments_would_exceed_remaining_charges() {
        var service = new DeleteProductChargeOpenAccountService(repository, totals("30.00", "50.00"), refresher);

        assertThrows(IllegalStateException.class, () -> service.execute(1L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar cuando se rechaza el borrado");
    }

    @Test
    void fails_when_charge_not_found() {
        var service = new DeleteProductChargeOpenAccountService(repository, totals("0", "0"), refresher);

        assertThrows(ProductChargeOpenAccountNotFoundException.class, () -> service.execute(99L));
    }
}
