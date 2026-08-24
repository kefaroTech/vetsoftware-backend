package com.vetsoftware.app.subscription.infrastructure.web.request;

import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una linea que se firma, con sus valores congelados.
 *
 * <p>
 * {@code includedQuantity} y {@code unitAmount} llegan aqui resueltos desde la
 * tarifa y se copian a la fila: no se releen despues. Ese es el motivo de que
 * viajen en el cuerpo en vez de deducirse en el servidor a partir del
 * {@code priceListId} — el contrato firmado tiene que sobrevivir a que la
 * tarifa cambie.
 */
public record SubscriptionItemLineRequest(@NotNull Long catalogItemId,
        @NotBlank @Size(max = 50) String itemCode, @NotBlank @Size(max = 120) String itemName,
        @NotNull SubscriptionItemType itemType, CapacityUnit capacityUnit,
        @NotNull @PositiveOrZero Integer includedQuantity, @NotNull TaxTreatment taxTreatment,
        @NotNull @Min(1) Integer quantity, @NotNull @PositiveOrZero BigDecimal unitAmount,
        @NotNull @PositiveOrZero BigDecimal taxRate, LocalDate effectiveFrom,
        LocalDate effectiveTo) {
}
