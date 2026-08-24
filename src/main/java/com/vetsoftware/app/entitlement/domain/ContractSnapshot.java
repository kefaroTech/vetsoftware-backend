package com.vetsoftware.app.entitlement.domain;

import java.util.List;

/**
 * El contrato de una empresa tal como lo necesita el recalculo: la cabecera y
 * <strong>todas</strong> sus lineas, las vigentes y las que ya terminaron.
 *
 * <p>
 * Las terminadas viajan a proposito: son las que producen la bajada a
 * {@code READ_ONLY}. Un recalculo que solo mirara las vigentes borraria el
 * permiso del modulo dado de baja en vez de degradarlo, y con el la unica forma
 * que tiene el cliente de consultar lo que ya escribio.
 */
public record ContractSnapshot(SubscriptionRef subscription, List<ModuleGrantLine> moduleLines,
        List<CapacityGrantLine> capacityLines) {

    public ContractSnapshot {
        if (subscription == null)
            throw new IllegalArgumentException("subscription is required");
        moduleLines = moduleLines == null ? List.of() : List.copyOf(moduleLines);
        capacityLines = capacityLines == null ? List.of() : List.copyOf(capacityLines);
    }
}
