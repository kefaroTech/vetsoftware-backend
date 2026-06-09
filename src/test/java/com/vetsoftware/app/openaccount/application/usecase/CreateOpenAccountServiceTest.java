package com.vetsoftware.app.openaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
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

class CreateOpenAccountServiceTest {

    private final OwnerRef owner = new OwnerRef(11L, "Juan Perez", "CC-123");
    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Empleado Uno");

    private OpenAccount savedOpenAccount;
    private boolean ownerHasAccount = false;

    private final OpenAccountRepository repository = new OpenAccountRepository() {
        @Override public OpenAccount save(OpenAccount openAccount) {
            savedOpenAccount = new OpenAccount(1L, openAccount.getOwner(),
                    openAccount.getTotalAmount(), openAccount.getPaidAmount(),
                    openAccount.getOutstandingAmount(), openAccount.getCompany(),
                    openAccount.getStatus(), openAccount.getCreatedBy(),
                    openAccount.getCreatedDate(), openAccount.isEnabled(),
                    openAccount.getClosedBy(), openAccount.getClosedAt(),
                    openAccount.getCloseReason(), openAccount.getVersion());
            return savedOpenAccount;
        }
        @Override public Optional<OpenAccount> findById(Long id) { return Optional.ofNullable(savedOpenAccount); }
        @Override public List<OpenAccount> findAll() { return List.of(); }
        @Override public List<OpenAccount> findAllByCompanyId(Long companyId) { return List.of(); }
        @Override public boolean existsActiveByOwnerId(Long ownerId) { return ownerHasAccount; }
        @Override public PageResult<OpenAccount> search(SearchOpenAccountsCommand command) {
            if (!ownerHasAccount) return new PageResult<>(List.of(), 0, 20, 0, 0);
            OpenAccount existing = new OpenAccount(99L, owner, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, company, OpenAccountStatus.OPEN, createdBy, LocalDateTime.now(),
                    true, null, null, null, null);
            return new PageResult<>(List.of(existing), 0, 20, 1, 1);
        }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private OwnerQueryPort ownerQueryPort(Optional<OwnerRef> result) { return ownerId -> result; }
    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) { return companyId -> result; }
    private EmployeeQueryPort employeeQueryPort(Optional<EmployeeRef> result) { return employeeId -> result; }

    private CreateOpenAccountCommand command() {
        return new CreateOpenAccountCommand(11L, 5L, 7L);
    }

    @Test
    void creates_open_account_with_zeroed_amounts() {
        var service = new CreateOpenAccountService(repository,
                ownerQueryPort(Optional.of(owner)),
                companyQueryPort(Optional.of(company)),
                employeeQueryPort(Optional.of(createdBy)));

        OpenAccountDto dto = service.execute(command());

        assertEquals(1L, dto.id());
        assertEquals(11L, dto.owner().id());
        assertEquals(5L, dto.company().id());
        assertEquals(7L, dto.createdBy().id());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.paidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.outstandingAmount()));
        assertTrue(dto.enabled());
    }

    @Test
    void returns_existing_when_owner_already_has_open_account() {
        ownerHasAccount = true;
        var service = new CreateOpenAccountService(repository,
                ownerQueryPort(Optional.of(owner)),
                companyQueryPort(Optional.of(company)),
                employeeQueryPort(Optional.of(createdBy)));

        OpenAccountDto dto = service.execute(command());

        assertEquals(99L, dto.id(), "devuelve la cuenta abierta existente (get-or-create)");
        assertNull(savedOpenAccount, "no debe crear ni guardar una cuenta nueva");
    }

    @Test
    void fails_when_owner_not_found() {
        var service = new CreateOpenAccountService(repository,
                ownerQueryPort(Optional.empty()),
                companyQueryPort(Optional.of(company)),
                employeeQueryPort(Optional.of(createdBy)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command()));
    }

    @Test
    void fails_when_company_not_found() {
        var service = new CreateOpenAccountService(repository,
                ownerQueryPort(Optional.of(owner)),
                companyQueryPort(Optional.empty()),
                employeeQueryPort(Optional.of(createdBy)));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command()));
    }

    @Test
    void fails_when_employee_not_found() {
        var service = new CreateOpenAccountService(repository,
                ownerQueryPort(Optional.of(owner)),
                companyQueryPort(Optional.of(company)),
                employeeQueryPort(Optional.empty()));

        assertThrows(IllegalArgumentException.class, () -> service.execute(command()));
    }
}
