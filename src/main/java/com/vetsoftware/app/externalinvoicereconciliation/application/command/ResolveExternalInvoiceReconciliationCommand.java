package com.vetsoftware.app.externalinvoicereconciliation.application.command;

/**
 * Cierra el expediente: firma, nota y periodo contable.
 *
 * <p>
 * Los cuatro campos de la resolucion van juntos ({@code chk_eir_resolved}); el
 * cuarto -{@code resolvedAt}- no esta aqui porque <strong>sale del reloj
 * inyectado</strong> y no del cliente: una fecha de resolucion que escribe el
 * llamante se puede antedatar a un periodo ya cerrado.
 *
 * @param postingPeriod
 *            periodo contable {@code YYYY-MM}. <strong>No tiene clave foranea y
 *            es una carencia declarada</strong>: {@code accounting_periods} es
 *            de otra capa y no existe en el arbol de changesets. El formato lo
 *            comprueba el dominio y lo vuelve a comprobar
 *            {@code chk_eir_resolved}
 */
public record ResolveExternalInvoiceReconciliationCommand(Long id, Long resolvedBySystemUserId,
        String resolutionNote, String postingPeriod) {
}
