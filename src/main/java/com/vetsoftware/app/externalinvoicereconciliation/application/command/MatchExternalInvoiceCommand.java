package com.vetsoftware.app.externalinvoicereconciliation.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registra la factura que emitio el tercero sobre una conciliacion abierta.
 *
 * <p>
 * <strong>Sin {@code companyId}, y no es un descuido.</strong> El {@code id} de
 * la conciliacion identifica una sola fila en toda la base, y este bloque no
 * tiene camino de tenant: el puerto esta cerrado a {@code hasRole('SYSTEM')} a
 * secas, y un principal SYSTEM no tiene empresa propia contra la que acotar.
 * Anadir aqui un {@code companyId} solo daria la falsa sensacion de una
 * comprobacion de propiedad que ningun principal de este camino puede aportar.
 *
 * <p>
 * <strong>El estado no viaja en el command.</strong> Lo decide el dominio a
 * partir de la diferencia: ver
 * {@code ExternalInvoiceReconciliation#classify(BigDecimal)}. Dejar que el
 * llamante eligiera entre {@code MATCHED}, {@code WITHIN_TOLERANCE} y
 * {@code MISMATCH} convertiria la regla de los dos pesos en una sugerencia.
 *
 * @param externalResolutionNumber
 *            el bloque de numeracion del tercero. Los cuatro campos del rango
 *            van juntos o ninguno ({@code chk_eir_resolution_range})
 */
public record MatchExternalInvoiceCommand(Long id, String externalInvoiceId, String externalCufe,
        BigDecimal externalTotal, BigDecimal externalTax, String externalResolutionNumber,
        Integer externalRangeFrom, Integer externalRangeTo, LocalDate resolutionValidUntil) {
}
