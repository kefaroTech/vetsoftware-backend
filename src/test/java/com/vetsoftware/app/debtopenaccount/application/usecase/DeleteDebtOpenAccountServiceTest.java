package com.vetsoftware.app.debtopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteDebtOpenAccountServiceTest {

    private final DebtOpenAccount existing = new DebtOpenAccount(1L, new BigDecimal("50.00"),
            PaymentMethod.CASH, new OpenAccountRef(10L, 5L), new EmployeeRef(7L, "Dr. House"),
            LocalDateTime.now(), true, false, null, null, null);

    private Long deletedId;
    private Long refreshedOpenAccountId;

    private final DebtOpenAccountRepository repository = new DebtOpenAccountRepository() {
        @Override public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) { return debtOpenAccount; }
        @Override public Optional<DebtOpenAccount> findById(Long id) {
            return id == 1L ? Optional.of(existing) : Optional.empty();
        }
        @Override public List<DebtOpenAccount> findAll() { return List.of(); }
        @Override public List<DebtOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) { deletedId = id; }
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = id -> refreshedOpenAccountId = id;

    @Test
    void deletes_and_refreshes_open_account() {
        var service = new DeleteDebtOpenAccountService(repository, refresher);

        service.execute(1L, 5L);

        assertEquals(1L, deletedId);
        assertEquals(10L, refreshedOpenAccountId);
    }

    @Test
    void fails_when_not_found() {
        var service = new DeleteDebtOpenAccountService(repository, refresher);

        assertThrows(DebtOpenAccountNotFoundException.class, () -> service.execute(99L, 5L));
    }

    @Test
    void rejects_delete_when_payment_belongs_to_other_company() {
        var service = new DeleteDebtOpenAccountService(repository, refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 999L));
        assertNull(deletedId, "no debe borrar un abono de otra empresa");
    }
}
