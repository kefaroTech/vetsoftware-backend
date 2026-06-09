package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReactivateGeneralChargeOpenAccountServiceTest {

    private final GeneralChargeOpenAccount existing = new GeneralChargeOpenAccount(1L, "Servicio extra",
            new BigDecimal("10.00"), new BigDecimal("1.00"), new TaxRef(7L, "IVA", new BigDecimal("19.00")),
            true, new BigDecimal("19.00"), new OpenAccountRef(10L, 5L),
            new EmployeeRef(3L, "Dr. House"), LocalDateTime.now(), true);

    private final List<Long> refreshed = new ArrayList<>();

    private GeneralChargeOpenAccountRepository repository(GeneralChargeOpenAccount stored) {
        return new GeneralChargeOpenAccountRepository() {
            @Override public GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge) { return charge; }
            @Override public Optional<GeneralChargeOpenAccount> findById(Long id) { return Optional.ofNullable(stored); }
            @Override public List<GeneralChargeOpenAccount> findAll() { return List.of(); }
            @Override public List<GeneralChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return stored == null ? 0 : 1; }
        };
    }

    private final OpenAccountRefresher refresher = refreshed::add;

    @Test
    void reactivates_and_refreshes_for_own_company() {
        var service = new ReactivateGeneralChargeOpenAccountService(repository(existing), refresher);

        GeneralChargeOpenAccountDto dto = service.execute(1L, 5L);

        assertEquals(1L, dto.id());
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void fails_when_not_found() {
        var service = new ReactivateGeneralChargeOpenAccountService(repository(null), refresher);

        assertThrows(GeneralChargeOpenAccountNotFoundException.class, () -> service.execute(99L, 5L));
    }

    @Test
    void rejects_reactivate_when_charge_belongs_to_other_company() {
        var service = new ReactivateGeneralChargeOpenAccountService(repository(existing), refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 999L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar al rechazar por empresa");
    }
}
