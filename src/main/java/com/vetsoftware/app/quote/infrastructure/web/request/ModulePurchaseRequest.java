package com.vetsoftware.app.quote.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Comprar uno o varios módulos del escaparate. <b>No lleva
 * {@code companyId}</b> (lo deriva el controller del principal) ni ningún
 * término económico: el precio de cada artículo lo resuelve el servidor contra
 * la lista de precios vigente, igual que en {@link SelfServeQuoteRequest}.
 *
 * @param catalogItemCodes
 *            rótulos del catálogo publicado, uno por línea a comprar.
 * @param billingCycle
 *            {@code MONTHLY} o {@code ANNUAL}, el mismo ciclo para toda la
 *            compra: no conviven dos ciclos en una suscripción.
 * @param paymentSourceId
 *            id del medio de pago ya registrado con el que se autoriza el
 *            cobro.
 * @param clientRequestId
 *            llave de idempotencia de la cotización de autoservicio que esta
 *            compra genera y acepta.
 */
public record ModulePurchaseRequest(@NotEmpty List<@NotBlank String> catalogItemCodes,
        @NotBlank @Pattern(regexp = "MONTHLY|ANNUAL") @Schema(allowableValues = {
                "MONTHLY", "ANNUAL"}) String billingCycle,
        @NotNull @Positive Long paymentSourceId,
        @NotBlank @Size(min = 1, max = 64) String clientRequestId) {
}
