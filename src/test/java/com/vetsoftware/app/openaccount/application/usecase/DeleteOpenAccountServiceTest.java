package com.vetsoftware.app.openaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
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

class DeleteOpenAccountServiceTest {

    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private Long deletedId;

    private OpenAccountRepository repository(OpenAccount existing) {
        return new OpenAccountRepository() {
            @Override public OpenAccount save(OpenAccount openAccount) { return openAccount; }
            @Override public Optional<OpenAccount> findById(Long id) { return Optional.ofNullable(existing); }
            @Override public List<OpenAccount> findAll() { return List.of(); }
            @Override public List<OpenAccount> findAllByCompanyId(Long companyId) { return List.of(); }
            @Override public boolean existsActiveByOwnerId(Long ownerId) { return false; }
            @Override public PageResult<OpenAccount> search(SearchOpenAccountsCommand command) {
                return new PageResult<>(List.of(), 0, 20, 0, 0);
            }
            @Override public void delete(Long id) { deletedId = id; }
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private OpenAccount account() {
        return new OpenAccount(1L, new OwnerRef(11L, "Juan Perez", "CC-123"),
                new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("60.00"),
                company, OpenAccountStatus.OPEN, new EmployeeRef(7L, "Empleado Uno"), LocalDateTime.now(), true,
                null, null, null, null);
    }

    @Test
    void deletes_for_own_company() {
        var service = new DeleteOpenAccountService(repository(account()));

        service.execute(1L, 5L);

        assertEquals(1L, deletedId);
    }

    @Test
    void fails_when_not_found() {
        var service = new DeleteOpenAccountService(repository(null));

        assertThrows(OpenAccountNotFoundException.class, () -> service.execute(99L, 5L));
    }

    @Test
    void rejects_delete_when_account_belongs_to_other_company() {
        var service = new DeleteOpenAccountService(repository(account()));

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 999L));
        assertNull(deletedId, "no debe borrar una cuenta de otra empresa");
    }
}
