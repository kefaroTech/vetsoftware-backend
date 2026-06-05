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
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateOpenAccountServiceTest {

    private final OwnerRef owner = new OwnerRef(11L, "Juan Perez", "CC-123");
    private final CompanyRef company = new CompanyRef(5L, "Acme", "900123");
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Empleado Uno");

    private OpenAccount savedOpenAccount;

    private final OpenAccountRepository repository = new OpenAccountRepository() {
        @Override public OpenAccount save(OpenAccount openAccount) {
            savedOpenAccount = new OpenAccount(1L, openAccount.getOwner(),
                    openAccount.getTotalAmount(), openAccount.getPaidAmount(),
                    openAccount.getOutstandingAmount(), openAccount.getCompany(),
                    openAccount.getCreatedBy(), openAccount.getCreatedDate(), openAccount.isEnabled());
            return savedOpenAccount;
        }
        @Override public Optional<OpenAccount> findById(Long id) { return Optional.ofNullable(savedOpenAccount); }
        @Override public List<OpenAccount> findAll() { return List.of(); }
        @Override public PageResult<OpenAccount> search(SearchOpenAccountsCommand command) {
            return new PageResult<>(List.of(), 0, 20, 0, 0);
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
