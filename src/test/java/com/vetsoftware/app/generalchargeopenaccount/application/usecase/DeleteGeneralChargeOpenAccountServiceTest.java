package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteGeneralChargeOpenAccountServiceTest {

    private final OpenAccountRef openAccount = new OpenAccountRef(10L, 5L);
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));
    private final EmployeeRef employee = new EmployeeRef(3L, "Dr. House");

    private final GeneralChargeOpenAccount existing = new GeneralChargeOpenAccount(1L, "Servicio extra",
            new BigDecimal("10.00"), new BigDecimal("1.00"), tax, true, new BigDecimal("19.00"),
            openAccount, employee, LocalDateTime.now(), true);

    private final List<Long> deleted = new ArrayList<>();
    private final List<Long> refreshed = new ArrayList<>();

    private final GeneralChargeOpenAccountRepository repository = new GeneralChargeOpenAccountRepository() {
        @Override public GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge) { return charge; }
        @Override public Optional<GeneralChargeOpenAccount> findById(Long id) {
            return id == 1L ? Optional.of(existing) : Optional.empty();
        }
        @Override public List<GeneralChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<GeneralChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
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
        var svc = new DeleteGeneralChargeOpenAccountService(repository, totals("80.00", "50.00"), refresher);

        svc.execute(1L, 5L);

        assertEquals(List.of(1L), deleted);
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void allows_delete_when_payments_equal_remaining_charges() {
        var svc = new DeleteGeneralChargeOpenAccountService(repository, totals("50.00", "50.00"), refresher);

        svc.execute(1L, 5L);

        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void rejects_delete_when_payments_would_exceed_remaining_charges() {
        var svc = new DeleteGeneralChargeOpenAccountService(repository, totals("30.00", "50.00"), refresher);

        assertThrows(IllegalStateException.class, () -> svc.execute(1L, 5L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar cuando se rechaza el borrado");
    }

    @Test
    void fails_when_charge_not_found() {
        var svc = new DeleteGeneralChargeOpenAccountService(repository, totals("0", "0"), refresher);

        assertThrows(GeneralChargeOpenAccountNotFoundException.class, () -> svc.execute(99L, 5L));
    }

    @Test
    void rejects_delete_when_charge_belongs_to_other_company() {
        var svc = new DeleteGeneralChargeOpenAccountService(repository, totals("80.00", "0"), refresher);

        assertThrows(IllegalArgumentException.class, () -> svc.execute(1L, 999L));
        assertTrue(deleted.isEmpty(), "no debe borrar un cargo de otra empresa");
    }
}
