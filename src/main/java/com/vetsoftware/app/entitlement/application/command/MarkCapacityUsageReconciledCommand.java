package com.vetsoftware.app.entitlement.application.command;

import java.time.LocalDateTime;

/**
 * Sellar el consumo de un contador: dejar escrito que se comprobo contra las
 * filas reales y cuadraba.
 *
 * <p>
 * <strong>El instante llega en el command y no se toma aqui.</strong> El sello
 * tiene que ser el mismo momento en que el barrido leyo las filas, no el
 * momento en que llega la escritura: entre las dos cosas puede haber pasado un
 * lote entero, y un sello posterior al recuento afirma frescura que nadie
 * comprobo.
 */
public record MarkCapacityUsageReconciledCommand(Long companyId, Long limitDimensionId,
        String periodKey, LocalDateTime reconciledAt) {

    public MarkCapacityUsageReconciledCommand {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (limitDimensionId == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (periodKey == null || periodKey.isBlank())
            throw new IllegalArgumentException("period key is required");
        if (reconciledAt == null)
            throw new IllegalArgumentException("reconciled at is required");
    }
}
