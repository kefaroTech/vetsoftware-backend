package com.vetsoftware.app.debtopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VoidDebtOpenAccountServiceTest {

    private final OpenAccountRef openAccount = new OpenAccountRef(10L, 5L);
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Dr. House");
    private final EmployeeRef voidedBy = new EmployeeRef(8L, "Dra. Cuddy");

    private DebtOpenAccount stored = payment();
    private DebtOpenAccount saved;
    private Long refreshedOpenAccountId;

    private DebtOpenAccount payment() {
        return new DebtOpenAccount(1L, new BigDecimal("50.00"), PaymentMethod.CASH,
                openAccount, createdBy, LocalDateTime.now(), true, false, null, null, null);
    }

    private final DebtOpenAccountRepository repository = new DebtOpenAccountRepository() {
        @Override public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) {
            saved = debtOpenAccount;
            return debtOpenAccount;
        }
        @Override public Optional<DebtOpenAccount> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<DebtOpenAccount> findAll() { return List.of(); }
        @Override public List<DebtOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = id -> refreshedOpenAccountId = id;

    private OpenAccountQueryPort openAccountQueryPort(boolean open) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return Optional.of(openAccount); }
            @Override public boolean isOpen(Long id) { return open; }
        };
    }
    private EmployeeQueryPort employeeQueryPort(Optional<EmployeeRef> result) { return id -> result; }

    private VoidDebtOpenAccountCommand command(Long companyId, String reason) {
        return new VoidDebtOpenAccountCommand(1L, companyId, 8L, reason);
    }

    @Test
    void voids_payment_with_reason_and_refreshes() {
        var service = new VoidDebtOpenAccountService(repository,
                openAccountQueryPort(true), employeeQueryPort(Optional.of(voidedBy)), refresher);

        DebtOpenAccountDto dto = service.execute(command(5L, "monto erróneo"));

        assertTrue(dto.voided());
        assertEquals(8L, dto.voidedBy().id());
        assertEquals("monto erróneo", dto.voidReason());
        assertNotNull(dto.voidedAt());
        assertTrue(saved.isVoided());
        assertEquals(10L, refreshedOpenAccountId);
    }

    @Test
    void fails_when_payment_not_found() {
        stored = null;
        var service = new VoidDebtOpenAccountService(repository,
                openAccountQueryPort(true), employeeQueryPort(Optional.of(voidedBy)), refresher);

        assertThrows(DebtOpenAccountNotFoundException.class, () -> service.execute(command(5L, "x")));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new VoidDebtOpenAccountService(repository,
                openAccountQueryPort(true), employeeQueryPort(Optional.of(voidedBy)), refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(999L, "x")));
    }

    @Test
    void fails_when_account_not_open() {
        var service = new VoidDebtOpenAccountService(repository,
                openAccountQueryPort(false), employeeQueryPort(Optional.of(voidedBy)), refresher);

        assertThrows(IllegalStateException.class, () -> service.execute(command(5L, "x")));
    }

    @Test
    void fails_when_reason_blank() {
        var service = new VoidDebtOpenAccountService(repository,
                openAccountQueryPort(true), employeeQueryPort(Optional.of(voidedBy)), refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(5L, "  ")));
    }

    @Test
    void fails_when_already_voided() {
        stored.voidPayment(voidedBy, "ya anulado");
        var service = new VoidDebtOpenAccountService(repository,
                openAccountQueryPort(true), employeeQueryPort(Optional.of(voidedBy)), refresher);

        assertThrows(DebtOpenAccountAlreadyVoidedException.class, () -> service.execute(command(5L, "otra vez")));
    }
}
