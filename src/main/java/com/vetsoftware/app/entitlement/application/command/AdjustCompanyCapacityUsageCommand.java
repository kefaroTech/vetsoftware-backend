package com.vetsoftware.app.entitlement.application.command;

import com.vetsoftware.app.entitlement.domain.CapacityUnit;

/**
 * Mover el consumo de una capacidad. {@code delta} es con signo: +1 al dar de
 * alta un usuario, -1 al darlo de baja.
 *
 * <p>
 * El movimiento se aplica con un {@code UPDATE ... SET used_quantity =
 * used_quantity + ?} atomico en el motor. <strong>Nunca</strong> leyendo,
 * modificando y guardando desde Java: dos altas simultaneas perderian una.
 */
public record AdjustCompanyCapacityUsageCommand(Long companyId, CapacityUnit capacityUnit,
        int delta) {

    public AdjustCompanyCapacityUsageCommand {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (capacityUnit == null)
            throw new IllegalArgumentException("capacity unit is required");
        if (delta == 0)
            throw new IllegalArgumentException("delta must not be zero");
    }
}
