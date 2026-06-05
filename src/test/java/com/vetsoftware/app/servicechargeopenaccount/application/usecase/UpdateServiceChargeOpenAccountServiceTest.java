package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.servicechargeopenaccount.application.command.UpdateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateServiceChargeOpenAccountServiceTest {

    private final AnimalRef animal = new AnimalRef(10L, "Firulais", "A-001");
    private final ServiceRef service = new ServiceRef(20L, "Consulta", new BigDecimal("50.00"));
    private final OpenAccountRef openAccount = new OpenAccountRef(30L, 5L);
    private final EmployeeRef employee = new EmployeeRef(40L, "Dr. House");

    private final ServiceChargeOpenAccount existing = new ServiceChargeOpenAccount(
            1L, animal, service, openAccount, employee, LocalDateTime.now(), true);

    private ServiceChargeOpenAccount savedCharge;

    private ServiceChargeOpenAccountRepository repository(boolean exists) {
        return new ServiceChargeOpenAccountRepository() {
            @Override public ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge) {
                savedCharge = new ServiceChargeOpenAccount(charge.getId(), charge.getAnimal(),
                        charge.getService(), charge.getOpenAccount(), charge.getCreatedBy(),
                        charge.getCreatedDate(), charge.isEnabled());
                return savedCharge;
            }
            @Override public Optional<ServiceChargeOpenAccount> findById(Long id) {
                return exists ? Optional.of(existing) : Optional.empty();
            }
            @Override public List<ServiceChargeOpenAccount> findAll() { return List.of(); }
            @Override public List<ServiceChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return 0; }
        };
    }

    private AnimalQueryPort animalQueryPort(Optional<AnimalRef> result) { return id -> result; }
    private ServiceQueryPort serviceQueryPort(Optional<ServiceRef> result) { return id -> result; }
    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) { return id -> result; }
    private final OpenAccountRefresher refresher = openAccountId -> { };

    private UpdateServiceChargeOpenAccountCommand command() {
        return new UpdateServiceChargeOpenAccountCommand(1L, 10L, 20L, 30L, 5L);
    }

    @Test
    void updates_service_charge() {
        var serviceUseCase = new UpdateServiceChargeOpenAccountService(repository(true),
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.of(openAccount)),
                refresher);

        ServiceChargeOpenAccountDto dto = serviceUseCase.execute(command());

        assertEquals(1L, dto.id());
        assertEquals(10L, dto.animal().id());
        assertEquals(20L, dto.service().id());
        assertEquals(30L, dto.openAccount().id());
    }

    @Test
    void fails_when_not_found() {
        var serviceUseCase = new UpdateServiceChargeOpenAccountService(repository(false),
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.of(openAccount)),
                refresher);

        assertThrows(ServiceChargeOpenAccountNotFoundException.class, () -> serviceUseCase.execute(command()));
    }

    @Test
    void fails_when_open_account_not_found() {
        var serviceUseCase = new UpdateServiceChargeOpenAccountService(repository(true),
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.empty()),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> serviceUseCase.execute(command()));
    }

    @Test
    void fails_when_company_mismatch() {
        var serviceUseCase = new UpdateServiceChargeOpenAccountService(repository(true),
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.of(new OpenAccountRef(30L, 999L))),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> serviceUseCase.execute(command()));
    }

    @Test
    void fails_when_service_not_found() {
        var serviceUseCase = new UpdateServiceChargeOpenAccountService(repository(true),
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.empty()),
                openAccountQueryPort(Optional.of(openAccount)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> serviceUseCase.execute(command()));
    }
}
