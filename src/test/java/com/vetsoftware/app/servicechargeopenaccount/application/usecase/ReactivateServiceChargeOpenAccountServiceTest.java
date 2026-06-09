package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReactivateServiceChargeOpenAccountServiceTest {

    private final ServiceChargeOpenAccount existing = new ServiceChargeOpenAccount(1L,
            new AnimalRef(10L, "Firulais", "A-001"),
            new ServiceRef(20L, "Consulta", new BigDecimal("50.00")),
            new BigDecimal("50.00"), new OpenAccountRef(30L, 5L),
            new EmployeeRef(40L, "Dr. House"), LocalDateTime.now(), true);

    private final List<Long> refreshed = new ArrayList<>();

    private ServiceChargeOpenAccountRepository repository(ServiceChargeOpenAccount stored) {
        return new ServiceChargeOpenAccountRepository() {
            @Override public ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge) { return charge; }
            @Override public Optional<ServiceChargeOpenAccount> findById(Long id) { return Optional.ofNullable(stored); }
            @Override public List<ServiceChargeOpenAccount> findAll() { return List.of(); }
            @Override public List<ServiceChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return stored == null ? 0 : 1; }
        };
    }

    private final OpenAccountRefresher refresher = refreshed::add;

    @Test
    void reactivates_and_refreshes_for_own_company() {
        var service = new ReactivateServiceChargeOpenAccountService(repository(existing), refresher);

        ServiceChargeOpenAccountDto dto = service.execute(1L, 5L);

        assertEquals(1L, dto.id());
        assertEquals(List.of(30L), refreshed);
    }

    @Test
    void fails_when_not_found() {
        var service = new ReactivateServiceChargeOpenAccountService(repository(null), refresher);

        assertThrows(ServiceChargeOpenAccountNotFoundException.class, () -> service.execute(99L, 5L));
    }

    @Test
    void rejects_reactivate_when_charge_belongs_to_other_company() {
        var service = new ReactivateServiceChargeOpenAccountService(repository(existing), refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 999L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar al rechazar por empresa");
    }
}
