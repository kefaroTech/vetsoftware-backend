package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.VoidProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_charge_open_account.void")
@Service
public class VoidProductChargeOpenAccountService implements VoidProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;

    public VoidProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
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
    public ProductChargeOpenAccountDto execute(VoidProductChargeOpenAccountCommand command) {
        ProductChargeOpenAccount charge = repository.findById(command.id())
            .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(command.id()));
        Long openAccountId = charge.getOpenAccount().id();
        if (!charge.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("product charge does not belong to company");
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
        ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(openAccountId);
        return dto;
    }
}
