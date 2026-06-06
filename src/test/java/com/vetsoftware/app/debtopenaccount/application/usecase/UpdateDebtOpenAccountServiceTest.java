package com.vetsoftware.app.debtopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateDebtOpenAccountServiceTest {

    private final EmployeeRef createdBy = new EmployeeRef(7L, "Dr. House");

    private DebtOpenAccount existing = new DebtOpenAccount(1L, new BigDecimal("50.00"),
            PaymentMethod.CASH, new OpenAccountRef(10L, 5L), createdBy, LocalDateTime.now(), true,
            false, null, null, null);
    private DebtOpenAccount saved;
    private final List<Long> refreshed = new ArrayList<>();

    private final DebtOpenAccountRepository repository = new DebtOpenAccountRepository() {
        @Override public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) {
            saved = debtOpenAccount;
            return debtOpenAccount;
        }
        @Override public Optional<DebtOpenAccount> findById(Long id) {
            return id == 1L ? Optional.of(existing) : Optional.empty();
        }
        @Override public List<DebtOpenAccount> findAll() { return List.of(); }
        @Override public List<DebtOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = refreshed::add;

    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return result; }
            @Override public boolean isOpen(Long id) { return result.isPresent(); }
        };
    }

    @Test
    void updates_debt_open_account_and_refreshes_same_account() {
        var service = new UpdateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        DebtOpenAccountDto dto = service.execute(
                new UpdateDebtOpenAccountCommand(1L, new BigDecimal("75.00"), "CARD", 10L, 5L));

        assertEquals(new BigDecimal("75.00"), dto.amount());
        assertEquals(PaymentMethod.CARD, dto.paymentMethod());
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void refreshes_both_accounts_when_open_account_changed() {
        var service = new UpdateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(new OpenAccountRef(20L, 5L))),
                refresher);

        service.execute(new UpdateDebtOpenAccountCommand(1L, new BigDecimal("75.00"), "CARD", 20L, 5L));

        assertTrue(refreshed.contains(20L));
        assertTrue(refreshed.contains(10L));
    }

    @Test
    void fails_when_debt_open_account_not_found() {
        var service = new UpdateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        assertThrows(RuntimeException.class, () -> service.execute(
                new UpdateDebtOpenAccountCommand(99L, new BigDecimal("75.00"), "CARD", 10L, 5L)));
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new UpdateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.empty()),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateDebtOpenAccountCommand(1L, new BigDecimal("75.00"), "CARD", 10L, 5L)));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new UpdateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateDebtOpenAccountCommand(1L, new BigDecimal("75.00"), "CARD", 10L, 999L)));
    }

    @Test
    void rejects_non_positive_amount_in_domain() {
        var service = new UpdateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateDebtOpenAccountCommand(1L, new BigDecimal("-1.00"), "CARD", 10L, 5L)));
    }
}
