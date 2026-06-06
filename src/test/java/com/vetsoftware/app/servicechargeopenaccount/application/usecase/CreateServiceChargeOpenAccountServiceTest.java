package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateServiceChargeOpenAccountServiceTest {

    private final AnimalRef animal = new AnimalRef(10L, "Firulais", "A-001");
    private final ServiceRef service = new ServiceRef(20L, "Consulta", new BigDecimal("50.00"));
    private final OpenAccountRef openAccount = new OpenAccountRef(30L, 5L);
    private final EmployeeRef employee = new EmployeeRef(40L, "Dr. House");

    private ServiceChargeOpenAccount savedCharge;

    private final ServiceChargeOpenAccountRepository repository = new ServiceChargeOpenAccountRepository() {
        @Override public ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge) {
            savedCharge = new ServiceChargeOpenAccount(1L, charge.getAnimal(), charge.getService(),
                    charge.getOpenAccount(), charge.getCreatedBy(), charge.getCreatedDate(), charge.isEnabled());
            return savedCharge;
        }
        @Override public Optional<ServiceChargeOpenAccount> findById(Long id) { return Optional.ofNullable(savedCharge); }
        @Override public List<ServiceChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<ServiceChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private AnimalQueryPort animalQueryPort(Optional<AnimalRef> result) { return id -> result; }
    private ServiceQueryPort serviceQueryPort(Optional<ServiceRef> result) { return id -> result; }
    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return result; }
            @Override public boolean isOpen(Long id) { return result.isPresent(); }
        };
    }
    private EmployeeQueryPort employeeQueryPort(Optional<EmployeeRef> result) { return id -> result; }
    private final OpenAccountRefresher refresher = openAccountId -> { };

    private CreateServiceChargeOpenAccountCommand command() {
        return new CreateServiceChargeOpenAccountCommand(10L, 20L, 30L, 5L, 40L);
    }

    @Test
    void creates_service_charge() {
        var serviceUseCase = new CreateServiceChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        ServiceChargeOpenAccountDto dto = serviceUseCase.execute(command());

        assertEquals(1L, dto.id());
        assertEquals(10L, dto.animal().id());
        assertEquals(20L, dto.service().id());
        assertEquals(30L, dto.openAccount().id());
        assertEquals(40L, dto.createdBy().id());
        assertTrue(dto.enabled());
    }

    @Test
    void fails_when_open_account_not_found() {
        var serviceUseCase = new CreateServiceChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.empty()),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> serviceUseCase.execute(command()));
    }

    @Test
    void fails_when_company_mismatch() {
        var serviceUseCase = new CreateServiceChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.of(service)),
                openAccountQueryPort(Optional.of(new OpenAccountRef(30L, 999L))),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> serviceUseCase.execute(command()));
    }

    @Test
    void fails_when_service_not_found() {
        var serviceUseCase = new CreateServiceChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                serviceQueryPort(Optional.empty()),
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(employee)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> serviceUseCase.execute(command()));
    }
}
