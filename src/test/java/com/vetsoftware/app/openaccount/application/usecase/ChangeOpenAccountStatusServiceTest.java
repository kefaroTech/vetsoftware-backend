package com.vetsoftware.app.openaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChangeOpenAccountStatusServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private OpenAccount saved;

    private OpenAccountRepository repository(OpenAccount existing) {
        return new OpenAccountRepository() {
            @Override public OpenAccount save(OpenAccount openAccount) { saved = openAccount; return openAccount; }
            @Override public Optional<OpenAccount> findById(Long id) { return Optional.ofNullable(existing); }
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

    private EmployeeQueryPort employeeQueryPort() {
        return id -> Optional.of(new EmployeeRef(7L, "Empleado Uno"));
    }

    private OpenAccount openAccount() {
        return new OpenAccount(1L, new OwnerRef(11L, "Juan Perez", "CC-123"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("0.00"),
                company, OpenAccountStatus.OPEN, new EmployeeRef(7L, "Empleado Uno"), LocalDateTime.now(), true,
                null, null, null, null);
    }

    @Test
    void closes_account_for_own_company() {
        var service = new ChangeOpenAccountStatusService(repository(openAccount()), employeeQueryPort());

        service.execute(new ChangeOpenAccountStatusCommand(1L, "CLOSE", 7L, null, 5L));

        assertEquals(OpenAccountStatus.CLOSE, saved.getStatus());
    }

    @Test
    void rejects_change_when_account_belongs_to_other_company() {
        var service = new ChangeOpenAccountStatusService(repository(openAccount()), employeeQueryPort());

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new ChangeOpenAccountStatusCommand(1L, "CLOSE", 7L, null, 999L)));
        assertNull(saved, "no debe cambiar el estado de una cuenta de otra empresa");
    }
}
