package com.vetsoftware.app.companytrialgrant.infrastructure.web.request;

import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Conceder la prueba de un artículo a una empresa.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: entra por la ruta
 * ({@code /system/company-trial-grants/companies/{companyId}}), como exige
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}.
 *
 * <p>
 * <strong>Sin fecha de fin y sin id de ventana</strong>, y las dos ausencias
 * son el mecanismo. El fin se calcula —el menor entre «alta más sus días» y el
 * fin de la ventana— y la ventana se resuelve por la empresa; dejar que el
 * llamante eligiera cualquiera de los dos es exactamente cómo se regalan meses
 * de software sin que ninguna fila esté mal.
 *
 * <p>
 * {@code policyTrialDays} y {@code policyTrialOutcome} son la política del
 * catálogo <em>congelada el día de la concesión</em>: si mañana se bajan de 30
 * a 14 días, a quien ya está probando no le cambia nada. {@code daysGranted}
 * son los que esta oferta concede, y una campaña puede bajar de la política,
 * nunca subir — eso lo comprueba el dominio, no este binder.
 */
public record GrantTrialRequest(
        @NotNull(message = "Debes indicar el artículo que se concede en prueba.") Long catalogItemId,
        @NotNull(message = "Debes indicar el día en que se concede la prueba.") LocalDate grantedOn,
        @NotNull(message = "Debes indicar cuántos días concede esta oferta.") @Positive(message = "La prueba tiene que conceder al menos un día.") Integer daysGranted,
        @NotNull(message = "Debes indicar cuántos días permite la política del artículo.") @Positive(message = "La política del artículo tiene que permitir al menos un día.") Integer policyTrialDays,
        @NotNull(message = "Debes indicar qué pasa al vencer la prueba.") TrialPolicyOutcome policyTrialOutcome,
        Long sourceQuoteId, Long grantingAmendmentId) {
}
