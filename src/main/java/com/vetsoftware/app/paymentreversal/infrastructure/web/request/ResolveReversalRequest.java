package com.vetsoftware.app.paymentreversal.infrastructure.web.request;

import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * {@code appliedAmount} no lleva {@code @NotNull} porque su obligatoriedad
 * depende del desenlace —lo exigen {@code ACCEPTED} y
 * {@code PARTIALLY_ACCEPTED} y lo prohiben los otros dos—, y eso es una
 * invariante del dominio, no una restriccion de campo. Lo que si se puede
 * afirmar aqui es que, si viene, es positivo.
 *
 * <p>
 * Sin {@code companyId} en el cuerpo ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}).
 */
public record ResolveReversalRequest(
        @NotNull(message = "Debes indicar el desenlace.") ReversalOutcome outcome,
        @Positive(message = "El importe aplicado debe ser mayor que cero.") BigDecimal appliedAmount,
        Long resultingRefundId) {
}
