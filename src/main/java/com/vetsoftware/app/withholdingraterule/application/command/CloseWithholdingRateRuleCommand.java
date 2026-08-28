package com.vetsoftware.app.withholdingraterule.application.command;

import java.time.LocalDate;

/**
 * Poner fecha de fin a una vigencia abierta.
 *
 * <p>
 * <strong>Cerrar no es borrar, y esa es toda la diferencia.</strong> La tarifa
 * que dejo de aplicarse el 1 de enero sigue siendo la correcta para una factura
 * de diciembre, asi que la fila se queda y lo que cambia es hasta cuando vale.
 * Borrarla o deshabilitarla dejaria sin explicacion las retenciones ya
 * calculadas.
 *
 * <p>
 * Es ademas <b>lo que libera el hueco</b>: mientras la regla no tiene fecha de
 * fin, la columna generada {@code current_rule_marker} vale el supuesto
 * completo y {@code uq_withholding_rate_rules_current} impide abrir otra igual.
 * Cerrar esta es el paso obligado antes de crear su relevo.
 *
 * @param validTo
 *            primer dia en que la tarifa <em>ya no</em> aplica. Estrictamente
 *            posterior a {@code validFrom}
 */
public record CloseWithholdingRateRuleCommand(Long id, LocalDate validTo) {
}
