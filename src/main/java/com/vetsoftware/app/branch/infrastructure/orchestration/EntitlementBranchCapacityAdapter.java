package com.vetsoftware.app.branch.infrastructure.orchestration;

import com.vetsoftware.app.branch.application.port.out.BranchCapacityPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import org.springframework.stereotype.Component;

@Component
public class EntitlementBranchCapacityAdapter implements BranchCapacityPort {
    /**
     * El codigo del eje en {@code limit_dimensions}. Antes era un valor del
     * enumerado cerrado de cuatro unidades, que es justo lo que habia que desplegar
     * para empezar a contar un eje nuevo; ahora es la clave con la que el contador
     * lo busca en el catalogo.
     */
    private static final String DIMENSION_CODE = "BRANCH";

    private final AdjustCompanyCapacityUsageUseCase adjustCapacity;

    public EntitlementBranchCapacityAdapter(AdjustCompanyCapacityUsageUseCase adjustCapacity) {
        this.adjustCapacity = adjustCapacity;
    }

    @Override
    public void reserve(Long companyId) {
        adjustCapacity.execute(new AdjustCompanyCapacityUsageCommand(companyId, DIMENSION_CODE, 1));
    }

    @Override
    public void release(Long companyId) {
        adjustCapacity
                .execute(new AdjustCompanyCapacityUsageCommand(companyId, DIMENSION_CODE, -1));
    }
}
