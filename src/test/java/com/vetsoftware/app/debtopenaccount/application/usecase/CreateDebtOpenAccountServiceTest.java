package com.vetsoftware.app.debtopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateDebtOpenAccountServiceTest {

    private final OpenAccountRef openAccount = new OpenAccountRef(10L, 5L);
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Dr. House");

    private DebtOpenAccount saved;
    private Long refreshedOpenAccountId;

    private final DebtOpenAccountRepository repository = new DebtOpenAccountRepository() {
        @Override public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) {
            saved = new DebtOpenAccount(1L, debtOpenAccount.getAmount(),
                    debtOpenAccount.getPaymentMethod(), debtOpenAccount.getOpenAccount(),
                    debtOpenAccount.getCreatedBy(), debtOpenAccount.getCreatedDate(),
                    debtOpenAccount.isEnabled());
            return saved;
        }
        @Override public Optional<DebtOpenAccount> findById(Long id) { return Optional.ofNullable(saved); }
        @Override public List<DebtOpenAccount> findAll() { return List.of(); }
        @Override public List<DebtOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = id -> refreshedOpenAccountId = id;

    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) { return id -> result; }
    private EmployeeQueryPort employeeQueryPort(Optional<EmployeeRef> result) { return id -> result; }

    private CreateDebtOpenAccountCommand command(String paymentMethod, Long companyId) {
        return new CreateDebtOpenAccountCommand(new BigDecimal("50.00"), paymentMethod, 10L, companyId, 7L);
    }

    @Test
    void creates_debt_open_account_and_refreshes() {
        var service = new CreateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        DebtOpenAccountDto dto = service.execute(command("CASH", 5L));

        assertEquals(1L, dto.id());
        assertEquals(new BigDecimal("50.00"), dto.amount());
        assertEquals(PaymentMethod.CASH, dto.paymentMethod());
        assertEquals(10L, dto.openAccount().id());
        assertEquals(7L, dto.createdBy().id());
        assertTrue(dto.enabled());
        assertEquals(10L, refreshedOpenAccountId);
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new CreateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.empty()),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command("CASH", 5L)));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new CreateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command("CASH", 999L)));
    }

    @Test
    void fails_when_employee_not_found() {
        var service = new CreateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.empty()),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command("CASH", 5L)));
    }

    @Test
    void fails_when_payment_method_invalid() {
        var service = new CreateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command("PAYPAL", 5L)));
    }

    @Test
    void rejects_non_positive_amount_in_domain() {
        var service = new CreateDebtOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateDebtOpenAccountCommand(BigDecimal.ZERO, "CASH", 10L, 5L, 7L)));
    }
}
