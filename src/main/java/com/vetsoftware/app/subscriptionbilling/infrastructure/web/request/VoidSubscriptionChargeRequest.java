package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La descripción del cargo de anulación.
 *
 * <p>
 * Es obligatoria porque <b>ese texto sale impreso</b>: el cargo negativo
 * aparece en la conciliación junto al que compensa, y «anulación» a secas no le
 * explica nada a quien lo lea dentro de dos años.
 */
public record VoidSubscriptionChargeRequest(@NotBlank @Size(max = 255) String description) {
}
