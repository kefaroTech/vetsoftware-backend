package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.generalchargeopenaccount.application.command.UpdateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateGeneralChargeOpenAccountServiceTest {

    private final OpenAccountRef openAccount = new OpenAccountRef(10L, 5L);
    private final TaxRef tax = new TaxRef(7L, "IVA", new BigDecimal("19.00"));
    private final EmployeeRef employee = new EmployeeRef(3L, "Dr. House");

    private GeneralChargeOpenAccount stored = new GeneralChargeOpenAccount(1L, "Original",
            new BigDecimal("10.00"), new BigDecimal("1.00"), tax, true, openAccount, employee,
            LocalDateTime.now(), true);
    private GeneralChargeOpenAccount savedCharge;

    private GeneralChargeOpenAccountRepository repository(GeneralChargeOpenAccount existing) {
        return new GeneralChargeOpenAccountRepository() {
            @Override public GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge) {
                savedCharge = charge;
                return charge;
            }
            @Override public Optional<GeneralChargeOpenAccount> findById(Long id) {
                return Optional.ofNullable(existing);
            }
            @Override public List<GeneralChargeOpenAccount> findAll() { return List.of(); }
            @Override public List<GeneralChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private final OpenAccountRefresher refresher = openAccountId -> { };

    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return result; }
            @Override public boolean isOpen(Long id) { return result.isPresent(); }
        };
    }
    private TaxQueryPort taxQueryPort(Optional<TaxRef> result) { return id -> result; }

    private UpdateGeneralChargeOpenAccountCommand command(Long taxId, Long companyId) {
        return new UpdateGeneralChargeOpenAccountCommand(1L, "Actualizado", new BigDecimal("20.00"),
                new BigDecimal("3.00"), taxId, false, 10L, companyId);
    }

    @Test
    void updates_charge_with_tax() {
        var service = new UpdateGeneralChargeOpenAccountService(repository(stored),
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                refresher);

        GeneralChargeOpenAccountDto dto = service.execute(command(7L, 5L));

        assertEquals(1L, dto.id());
        assertEquals("Actualizado", dto.name());
        assertEquals(new BigDecimal("20.00"), dto.unitAmount());
        assertNotNull(dto.tax());
    }

    @Test
    void updates_charge_without_tax() {
        var service = new UpdateGeneralChargeOpenAccountService(repository(stored),
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.empty()),
                refresher);

        GeneralChargeOpenAccountDto dto = service.execute(command(null, 5L));

        assertNull(dto.tax());
    }

    @Test
    void fails_when_charge_not_found() {
        var service = new UpdateGeneralChargeOpenAccountService(repository(null),
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                refresher);

        assertThrows(GeneralChargeOpenAccountNotFoundException.class, () -> service.execute(command(7L, 5L)));
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new UpdateGeneralChargeOpenAccountService(repository(stored),
                openAccountQueryPort(Optional.empty()),
                taxQueryPort(Optional.of(tax)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L, 5L)));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new UpdateGeneralChargeOpenAccountService(repository(stored),
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(7L, 999L)));
    }

    @Test
    void rejects_negative_quantity_in_domain() {
        var service = new UpdateGeneralChargeOpenAccountService(repository(stored),
                openAccountQueryPort(Optional.of(openAccount)),
                taxQueryPort(Optional.of(tax)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateGeneralChargeOpenAccountCommand(1L, "Actualizado", new BigDecimal("20.00"),
                        new BigDecimal("-3.00"), 7L, false, 10L, 5L)));
    }
}
