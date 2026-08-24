package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Devengar un cargo.
 *
 * <p>
 * <b>Sin {@code companyId}, como todo cuerpo de este proyecto.</b> La empresa
 * llega por la ruta ({@code /companies/{companyId}/...}) en los endpoints de
 * plataforma y del principal en los del tenant. Un {@code companyId} escrito
 * por el cliente en el JSON convertiría cualquier validación de propiedad en
 * una comparación del número consigo mismo.
 *
 * <p>
 * <b>{@code subtotalAmount} va con signo y por eso no lleva
 * {@code @PositiveOrZero}</b>: el signo lo decide el {@code chargeType}, y esa
 * regla la comprueba el dominio, que es donde puede leer los dos campos a la
 * vez.
 */
public record CreateSubscriptionChargeRequest(@NotNull Long subscriptionId, Long subscriptionItemId,
        @NotNull ChargeType chargeType, @NotBlank @Size(max = 255) String description,
        @NotNull LocalDate servicePeriodStart, @NotNull LocalDate servicePeriodEnd,
        @NotNull @DecimalMin(value = "0.001") @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal unitAmount,
        @NotNull @Digits(integer = 17, fraction = 2) BigDecimal subtotalAmount,
        @NotNull @PositiveOrZero @DecimalMin(value = "0.00") @Digits(integer = 3, fraction = 2) BigDecimal taxRate,
        @NotNull TaxTreatment taxTreatment, @PositiveOrZero Integer prorationDays,
        @PositiveOrZero Integer periodDays, Long amendmentId) {
}
