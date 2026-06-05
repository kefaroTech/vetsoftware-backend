package com.vetsoftware.app.openaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.command.UpdateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateOpenAccountServiceTest {

    private final OwnerRef originalOwner = new OwnerRef(11L, "Juan Perez", "CC-123");
    private final OwnerRef newOwner = new OwnerRef(22L, "Maria Lopez", "CC-456");
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

    private OwnerQueryPort ownerQueryPort(Optional<OwnerRef> result) { return ownerId -> result; }

    private OpenAccount existingAccount() {
        return new OpenAccount(1L, originalOwner, new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("60.00"), company, OpenAccountStatus.OPEN, createdBy, LocalDateTime.now(), true);
    }

    @Test
    void updates_owner_only() {
        var service = new UpdateOpenAccountService(
                repository(existingAccount()), ownerQueryPort(Optional.of(newOwner)));

        OpenAccountDto dto = service.execute(new UpdateOpenAccountCommand(1L, 22L, 5L));

        assertEquals(22L, dto.owner().id());
        assertEquals(22L, savedOpenAccount.getOwner().id());
        // amounts untouched
        assertEquals(0, new BigDecimal("100.00").compareTo(dto.totalAmount()));
        assertEquals(0, new BigDecimal("40.00").compareTo(dto.paidAmount()));
        assertEquals(0, new BigDecimal("60.00").compareTo(dto.outstandingAmount()));
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new UpdateOpenAccountService(
                repository(null), ownerQueryPort(Optional.of(newOwner)));

        assertThrows(OpenAccountNotFoundException.class,
                () -> service.execute(new UpdateOpenAccountCommand(99L, 22L, 5L)));
    }

    @Test
    void fails_when_owner_not_found() {
        var service = new UpdateOpenAccountService(
                repository(existingAccount()), ownerQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new UpdateOpenAccountCommand(1L, 22L, 5L)));
    }
}
