package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Una linea del contrato proyectada sobre un submodulo concreto: "este cliente
 * tiene contratado esto, desde esta fecha y hasta esta otra".
 *
 * <p>
 * {@code readOnlyCapable} viaja aqui y no en {@link SubModuleRef} a proposito:
 * es un dato del catalogo que solo hace falta <em>mientras se calcula</em>, no
 * algo que el permiso guarde.
 */
public record ModuleGrantLine(Long subscriptionItemId, SubModuleRef subModule,
        boolean readOnlyCapable, LocalDate effectiveFrom, LocalDate effectiveTo, boolean core) {

    public ModuleGrantLine {
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (subModule == null)
            throw new IllegalArgumentException("sub module is required");
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effective from is required");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("effective to cannot precede effective from");
    }

    /**
     * Vigente el dia indicado: ya empezo y todavia no ha terminado.
     * {@code effective_to} es <strong>exclusiva</strong>, igual que en la consulta
     * de vigencia de {@code subscription_items}.
     */
    public boolean isCurrentOn(LocalDate day) {
        return !effectiveFrom.isAfter(day) && (effectiveTo == null || effectiveTo.isAfter(day));
    }

    /** Ya termino: la baja del modulo ocurrio y no es una linea futura. */
    public boolean hasEndedOn(LocalDate day) {
        return effectiveTo != null && !effectiveTo.isAfter(day);
    }
}
