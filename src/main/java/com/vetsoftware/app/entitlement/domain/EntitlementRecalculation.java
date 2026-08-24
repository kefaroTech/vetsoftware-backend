package com.vetsoftware.app.entitlement.domain;

import java.util.List;

/**
 * Lo que el contrato dice que esta empresa puede usar y cuanto puede consumir.
 * Es el estado completo de la empresa, no un delta: el recalculo reconstruye la
 * tabla entera para ella.
 */
public record EntitlementRecalculation(List<CompanyEntitlement> entitlements,
        List<CompanyCapacity> capacities) {

    public EntitlementRecalculation {
        entitlements = entitlements == null ? List.of() : List.copyOf(entitlements);
        capacities = capacities == null ? List.of() : List.copyOf(capacities);
    }
}
