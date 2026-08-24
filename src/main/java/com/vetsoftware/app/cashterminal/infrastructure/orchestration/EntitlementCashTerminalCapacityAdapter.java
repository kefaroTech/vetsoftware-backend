package com.vetsoftware.app.cashterminal.infrastructure.orchestration;

import com.vetsoftware.app.cashterminal.application.port.out.CashTerminalCapacityPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import org.springframework.stereotype.Component;

@Component
public class EntitlementCashTerminalCapacityAdapter implements CashTerminalCapacityPort {
    private final AdjustCompanyCapacityUsageUseCase adjustCapacity;

    public EntitlementCashTerminalCapacityAdapter(
            AdjustCompanyCapacityUsageUseCase adjustCapacity) {
        this.adjustCapacity = adjustCapacity;
    }

    @Override
    public void reserve(Long companyId) {
        adjustCapacity.execute(
                new AdjustCompanyCapacityUsageCommand(companyId, CapacityUnit.TERMINAL, 1));
    }

    @Override
    public void release(Long companyId) {
        adjustCapacity.execute(
                new AdjustCompanyCapacityUsageCommand(companyId, CapacityUnit.TERMINAL, -1));
    }
}
