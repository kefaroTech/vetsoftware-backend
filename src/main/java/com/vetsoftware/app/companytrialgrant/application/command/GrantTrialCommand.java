package com.vetsoftware.app.companytrialgrant.application.command;

import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import java.time.LocalDate;

/**
 * Conceder la prueba de un artículo.
 *
 * <p>
 * <strong>No lleva fecha de fin ni id de ventana.</strong> El fin se calcula
 * —menor entre «alta más sus días» y el fin de la ventana— y la ventana se
 * resuelve por la empresa: dejar que el llamante eligiera cualquiera de los dos
 * es exactamente cómo se regalan meses de software sin que ninguna fila esté
 * mal.
 *
 * @param policyTrialDays
 *            los días que el catálogo permite, congelados aquí el día de la
 *            concesión: si mañana se bajan de 30 a 14, a quien ya está probando
 *            no le cambia nada
 * @param daysGranted
 *            los días que esta oferta concede. Una campaña puede bajar de la
 *            política, nunca subir
 */
public record GrantTrialCommand(Long companyId, Long catalogItemId, LocalDate grantedOn,
        int daysGranted, int policyTrialDays, TrialPolicyOutcome policyTrialOutcome,
        Long sourceQuoteId, Long grantingAmendmentId) {
}
