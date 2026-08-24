package com.vetsoftware.app.employee.infrastructure.orchestration;

import com.vetsoftware.app.employee.application.port.out.EmployeeCapacityPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import org.springframework.stereotype.Component;

@Component
public class EntitlementEmployeeCapacityAdapter implements EmployeeCapacityPort {
    private final AdjustCompanyCapacityUsageUseCase adjustCapacity;

    public EntitlementEmployeeCapacityAdapter(AdjustCompanyCapacityUsageUseCase adjustCapacity) {
        this.adjustCapacity = adjustCapacity;
    }

    @Override
    public void reserve(Long companyId) {
        adjustCapacity
                .execute(new AdjustCompanyCapacityUsageCommand(companyId, CapacityUnit.USER, 1));
    }

    @Override
    public void release(Long companyId) {
        adjustCapacity
                .execute(new AdjustCompanyCapacityUsageCommand(companyId, CapacityUnit.USER, -1));
    }
}
