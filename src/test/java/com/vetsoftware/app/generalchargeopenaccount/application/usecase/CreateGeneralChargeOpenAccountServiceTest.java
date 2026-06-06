package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateGeneralChargeOpenAccountServiceTest {

    private final OpenAccountRef openAccount = new OpenAccountRef(10L, 5L);
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));
    private final EmployeeRef employee = new EmployeeRef(3L, "Dr. House");

    private GeneralChargeOpenAccount savedCharge;

    private final GeneralChargeOpenAccountRepository repository = new GeneralChargeOpenAccountRepository() {
        @Override public GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge) {
            savedCharge = new GeneralChargeOpenAccount(1L, charge.getName(), charge.getUnitAmount(),
                    charge.getQuantity(), charge.getTax(), charge.isHasTax(), charge.getTaxPercentage(),
                    charge.getOpenAccount(), charge.getCreatedBy(), charge.getCreatedDate(), charge.isEnabled(),
                    charge.isVoided(), charge.getVoidedBy(), charge.getVoidedAt(), charge.getVoidReason());
            return savedCharge;
        }
        @Override public Optional<GeneralChargeOpenAccount> findById(Long id) { return Optional.ofNullable(savedCharge); }
        @Override public List<GeneralChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<GeneralChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = openAccountId -> { };

    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return result; }
            @Override public boolean isOpen(Long id) { return result.isPresent(); }
            @Override public java.math.BigDecimal outstandingAmount(Long id) { return java.math.BigDecimal.ZERO; }
        };
    }
    private TaxQueryPort taxQueryPort(Optional<TaxRef> result) { return id -> result; }
    private EmployeeQueryPort employeeQueryPort(Optional<EmployeeRef> result) { return id -> result; }

    private CreateGeneralChargeOpenAccountCommand command(Long taxId, Long companyId) {
        return new CreateGeneralChargeOpenAccountCommand("Consulta extra", new BigDecimal("50.00"),
                new BigDecimal("2.00"), taxId, true, 10L, companyId, 3L);
    }

    @Test
    void creates_charge_with_tax() {
        var service = new CreateGeneralChargeOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        GeneralChargeOpenAccountDto dto = service.execute(command(7L, 5L));

        assertEquals(1L, dto.id());
        assertEquals("Consulta extra", dto.name());
        assertNotNull(dto.tax());
        assertEquals(7L, dto.tax().id());
        assertEquals(new BigDecimal("19.00"), dto.taxPercentage());
        assertEquals(10L, dto.openAccount().id());
        assertEquals(3L, dto.createdBy().id());
        assertTrue(dto.enabled());
    }

    @Test
    void creates_charge_without_tax() {
        var service = new CreateGeneralChargeOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.empty()),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        GeneralChargeOpenAccountDto dto = service.execute(command(null, 5L));

        assertEquals(1L, dto.id());
        assertNull(dto.tax());
        assertEquals(10L, dto.openAccount().id());
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new CreateGeneralChargeOpenAccountService(repository,
                openAccountQueryPort(Optional.empty()),
                taxQueryPort(Optional.of(tax)),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L, 5L)));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new CreateGeneralChargeOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L, 999L)));
    }

    @Test
    void rejects_negative_quantity_in_domain() {
        var service = new CreateGeneralChargeOpenAccountService(repository,
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new CreateGeneralChargeOpenAccountCommand("Consulta extra", new BigDecimal("50.00"),
                        new BigDecimal("-1.00"), 7L, true, 10L, 5L, 3L)));
    }
}
