package com.vetsoftware.app.openaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountTotalsPort;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecalculateOpenAccountServiceTest {

    private final OwnerRef owner = new OwnerRef(11L, "Juan Perez", "CC-123");
    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Empleado Uno");

    private OpenAccount stored;
    private OpenAccount savedOpenAccount;

    private OpenAccountRepository repository(OpenAccount existing) {
        this.stored = existing;
        return new OpenAccountRepository() {
            @Override public OpenAccount save(OpenAccount openAccount) {
                savedOpenAccount = openAccount;
                return openAccount;
            }
            @Override public Optional<OpenAccount> findById(Long id) { return Optional.ofNullable(stored); }
            @Override public List<OpenAccount> findAll() { return List.of(); }
            @Override public List<OpenAccount> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public boolean existsActiveByOwnerId(Long ownerId) { return false; }
            @Override public PageResult<OpenAccount> search(SearchOpenAccountsCommand command) {
                return new PageResult<>(List.of(), 0, 20, 0, 0);
            }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private OpenAccountTotalsPort totalsPort(BigDecimal total, BigDecimal paid) {
        return new OpenAccountTotalsPort() {
            @Override public BigDecimal totalCharges(Long openAccountId) { return total; }
            @Override public BigDecimal totalPayments(Long openAccountId) { return paid; }
        };
    }

    private OpenAccount existingAccount() {
        return new OpenAccount(1L, owner, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                company, createdBy, LocalDateTime.now(), true);
    }

    @Test
    void recalculates_outstanding_as_total_minus_paid_and_saves() {
        var service = new RecalculateOpenAccountService(
                repository(existingAccount()),
                totalsPort(new BigDecimal("150.00"), new BigDecimal("90.00")));

        service.recalculate(1L);

        assertNotNull(savedOpenAccount);
        assertEquals(0, new BigDecimal("150.00").compareTo(savedOpenAccount.getTotalAmount()));
        assertEquals(0, new BigDecimal("90.00").compareTo(savedOpenAccount.getPaidAmount()));
        assertEquals(0, new BigDecimal("60.00").compareTo(savedOpenAccount.getOutstandingAmount()));
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new RecalculateOpenAccountService(
                repository(null),
                totalsPort(new BigDecimal("150.00"), new BigDecimal("90.00")));

        assertThrows(OpenAccountNotFoundException.class, () -> service.recalculate(99L));
    }
}
