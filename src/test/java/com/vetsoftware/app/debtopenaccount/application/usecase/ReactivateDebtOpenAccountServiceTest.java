package com.vetsoftware.app.debtopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReactivateDebtOpenAccountServiceTest {

    private final DebtOpenAccount existing = new DebtOpenAccount(1L, new BigDecimal("50.00"),
            PaymentMethod.CASH, new OpenAccountRef(10L, 5L), new EmployeeRef(7L, "Dr. House"),
            LocalDateTime.now(), true, false, null, null, null);

    private final List<Long> refreshed = new ArrayList<>();

    private DebtOpenAccountRepository repository(DebtOpenAccount stored) {
        return new DebtOpenAccountRepository() {
            @Override public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) { return debtOpenAccount; }
            @Override public Optional<DebtOpenAccount> findById(Long id) { return Optional.ofNullable(stored); }
            @Override public List<DebtOpenAccount> findAll() { return List.of(); }
            @Override public List<DebtOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return stored == null ? 0 : 1; }
        };
    }

    private final OpenAccountRefresher refresher = refreshed::add;

    @Test
    void reactivates_and_refreshes_for_own_company() {
        var service = new ReactivateDebtOpenAccountService(repository(existing), refresher);

        DebtOpenAccountDto dto = service.execute(1L, 5L);

        assertEquals(1L, dto.id());
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void fails_when_not_found() {
        var service = new ReactivateDebtOpenAccountService(repository(null), refresher);

        assertThrows(DebtOpenAccountNotFoundException.class, () -> service.execute(99L, 5L));
    }

    @Test
    void rejects_reactivate_when_payment_belongs_to_other_company() {
        var service = new ReactivateDebtOpenAccountService(repository(existing), refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 999L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar al rechazar por empresa");
    }
}
