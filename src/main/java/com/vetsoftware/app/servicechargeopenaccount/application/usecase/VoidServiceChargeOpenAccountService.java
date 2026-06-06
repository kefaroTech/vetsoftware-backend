package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.command.VoidServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.VoidServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_charge_open_account.void")
@Service
public class VoidServiceChargeOpenAccountService implements VoidServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;

    public VoidServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                               OpenAccountQueryPort openAccountQueryPort,
                                               EmployeeQueryPort employeeQueryPort,
                                               OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public ServiceChargeOpenAccountDto execute(VoidServiceChargeOpenAccountCommand command) {
        ServiceChargeOpenAccount charge = repository.findById(command.id())
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(command.id()));
        Long openAccountId = charge.getOpenAccount().id();
        if (!charge.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("service charge does not belong to company");
        }
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }
        // No se puede anular un cargo si eso dejaría el saldo pendiente negativo (hay abonos que
        // ya cubren más de lo que quedaría). Regla: monto del cargo <= saldo pendiente actual.
        BigDecimal outstanding = openAccountQueryPort.outstandingAmount(openAccountId);
        if (charge.getUnitPrice().compareTo(outstanding) > 0) {
            throw new IllegalStateException(
                "No se puede anular el cargo: el saldo pendiente quedaría negativo. "
                + "Hay abonos que lo cubren; anula primero los abonos necesarios.");
        }
        EmployeeRef voidedBy = employeeQueryPort.findById(command.voidedById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.voidedById()));

        charge.voidCharge(voidedBy, command.reason());
        ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(openAccountId);
        return dto;
    }
}
