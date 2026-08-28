package com.vetsoftware.app.companylimitevent.application.command;

/**
 * Corregir el consumo de un contador desde plataforma, con motivo y firma.
 *
 * <p>
 * <strong>La firma va en el command y no se deriva del contexto.</strong> Quien
 * corrige un contador tiene que quedar escrito con nombre: es lo que separa una
 * corrección auditable de un {@code UPDATE} a mano en producción.
 */
public record AdjustCompanyUsageCommand(Long companyId, Long limitDimensionId, String capacityUnit,
        int delta, Long systemUserId, String reasonCode, String reason) {
}
