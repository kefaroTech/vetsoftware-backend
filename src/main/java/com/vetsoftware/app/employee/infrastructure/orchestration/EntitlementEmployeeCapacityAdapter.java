package com.vetsoftware.app.employee.infrastructure.orchestration;

import com.vetsoftware.app.employee.application.port.out.EmployeeCapacityPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import org.springframework.stereotype.Component;

@Component
public class EntitlementEmployeeCapacityAdapter implements EmployeeCapacityPort {
    /**
     * El codigo del eje en {@code limit_dimensions}. Antes era un valor del
     * enumerado cerrado de cuatro unidades, que es justo lo que habia que desplegar
     * para empezar a contar un eje nuevo; ahora es la clave con la que el contador
     * lo busca en el catalogo.
     */
    private static final String DIMENSION_CODE = "USER";

    private final AdjustCompanyCapacityUsageUseCase adjustCapacity;

    public EntitlementEmployeeCapacityAdapter(AdjustCompanyCapacityUsageUseCase adjustCapacity) {
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
