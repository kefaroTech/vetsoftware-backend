package com.vetsoftware.app.branch.infrastructure.orchestration;

import com.vetsoftware.app.branch.application.port.out.BranchCapacityPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import org.springframework.stereotype.Component;

@Component
public class EntitlementBranchCapacityAdapter implements BranchCapacityPort {
    private final AdjustCompanyCapacityUsageUseCase adjustCapacity;

    public EntitlementBranchCapacityAdapter(AdjustCompanyCapacityUsageUseCase adjustCapacity) {
        this.adjustCapacity = adjustCapacity;
    }

    @Override
    public void reserve(Long companyId) {
        adjustCapacity
                .execute(new AdjustCompanyCapacityUsageCommand(companyId, CapacityUnit.BRANCH, 1));
    }

    @Override
    public void release(Long companyId) {
        adjustCapacity
                .execute(new AdjustCompanyCapacityUsageCommand(companyId, CapacityUnit.BRANCH, -1));
    }
}
