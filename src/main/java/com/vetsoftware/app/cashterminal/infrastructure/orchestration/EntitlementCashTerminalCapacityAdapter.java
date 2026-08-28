package com.vetsoftware.app.cashterminal.infrastructure.orchestration;

import com.vetsoftware.app.cashterminal.application.port.out.CashTerminalCapacityPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import org.springframework.stereotype.Component;

@Component
public class EntitlementCashTerminalCapacityAdapter implements CashTerminalCapacityPort {
    /**
     * El codigo del eje en {@code limit_dimensions}. Antes era un valor del
     * enumerado cerrado de cuatro unidades, que es justo lo que habia que desplegar
     * para empezar a contar un eje nuevo; ahora es la clave con la que el contador
     * lo busca en el catalogo.
     */
    private static final String DIMENSION_CODE = "TERMINAL";

    private final AdjustCompanyCapacityUsageUseCase adjustCapacity;

    public EntitlementCashTerminalCapacityAdapter(
            AdjustCompanyCapacityUsageUseCase adjustCapacity) {
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
