package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteServiceChargeOpenAccountServiceTest {

    private final AnimalRef animal = new AnimalRef(10L, "Firulais", "A-001");
    private final ServiceRef service = new ServiceRef(20L, "Consulta", new BigDecimal("50.00"));
    private final OpenAccountRef openAccount = new OpenAccountRef(30L, 5L);
    private final EmployeeRef employee = new EmployeeRef(40L, "Dr. House");

    private final ServiceChargeOpenAccount existing = new ServiceChargeOpenAccount(
            1L, animal, service, new BigDecimal("50.00"), openAccount, employee, LocalDateTime.now(), true);

    private final List<Long> deleted = new ArrayList<>();
    private final List<Long> refreshed = new ArrayList<>();

    private final ServiceChargeOpenAccountRepository repository = new ServiceChargeOpenAccountRepository() {
        @Override public ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge) { return charge; }
        @Override public Optional<ServiceChargeOpenAccount> findById(Long id) {
            return id == 1L ? Optional.of(existing) : Optional.empty();
        }
        @Override public List<ServiceChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<ServiceChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
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
        var svc = new DeleteServiceChargeOpenAccountService(repository, totals("80.00", "50.00"), refresher);

        svc.execute(1L);

        assertEquals(List.of(1L), deleted);
        assertEquals(List.of(30L), refreshed);
    }

    @Test
    void allows_delete_when_payments_equal_remaining_charges() {
        var svc = new DeleteServiceChargeOpenAccountService(repository, totals("50.00", "50.00"), refresher);

        svc.execute(1L);

        assertEquals(List.of(30L), refreshed);
    }

    @Test
    void rejects_delete_when_payments_would_exceed_remaining_charges() {
        var svc = new DeleteServiceChargeOpenAccountService(repository, totals("30.00", "50.00"), refresher);

        assertThrows(IllegalStateException.class, () -> svc.execute(1L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar cuando se rechaza el borrado");
    }

    @Test
    void fails_when_charge_not_found() {
        var svc = new DeleteServiceChargeOpenAccountService(repository, totals("0", "0"), refresher);

        assertThrows(ServiceChargeOpenAccountNotFoundException.class, () -> svc.execute(99L));
    }
}
