package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.SnapshotReason;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Deja constancia de <strong>como quedo</strong> una empresa tras un recalculo.
 *
 * <p>
 * Es una foto <strong>por recalculo, no por permiso</strong> (R-ENT-15): la
 * pregunta que contesta es cual era el acceso de esta clinica en una fecha
 * dada, y esa se responde con el estado entero, no con una fila suelta. Sin
 * ella, la unica forma de reconstruir un permiso pasado es volver a correr el
 * calculo sobre un contrato que ya cambio, que es justamente lo que no se puede
 * hacer.
 *
 * <p>
 * La foto se escribe <strong>dentro de la transaccion del recalculo</strong>.
 * Si el recalculo se revierte, la foto se va con el: una foto de un estado que
 * nunca llego a existir es peor que no tener foto, porque nadie sabe cual de
 * las dos miente.
 */
public interface EntitlementSnapshotPort {

    /**
     * @param entitlements
     *            las filas derivadas recien escritas, ya en su orden estable
     * @param capacities
     *            los contadores tal como quedaron tras conciliar
     */
    void record(Long companyId, LocalDateTime recalculatedAt, SnapshotReason reason,
            List<CompanyEntitlement> entitlements, List<CompanyCapacity> capacities);
}
