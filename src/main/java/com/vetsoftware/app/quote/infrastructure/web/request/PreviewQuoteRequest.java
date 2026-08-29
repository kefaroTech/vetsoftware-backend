package com.vetsoftware.app.quote.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Un prospecto pregunta cuanto le costaria su seleccion.
 *
 * <p>
 * <strong>No lleva ningun termino economico</strong>, igual que
 * {@link SelfServeQuoteRequest} y por lo mismo: el precio lo pone el servidor y
 * la unica forma de garantizarlo es que el cliente no tenga donde ponerlo.
 * Tampoco lleva empresa — aqui no hay ninguna, es anonimo.
 *
 * <p>
 * El {@code @Valid} de la lista no es decorativo: sin el, las restricciones de
 * {@link SelfServeQuoteLineRequest} estan escritas y no se evaluan nunca
 * ({@code CUERPO_CON_RESTRICCIONES_SE_VALIDA}).
 *
 * <p>
 * Reusa {@link SelfServeQuoteLineRequest} a proposito: una linea de la vista
 * previa y una de la contratacion son la misma cosa —un rotulo y una cantidad—,
 * y separarlas invitaria a que una de las dos aceptara algo que la otra no.
 */
public record PreviewQuoteRequest(
        @NotBlank @Pattern(regexp = "MONTHLY|ANNUAL") @Schema(allowableValues = {
                "MONTHLY", "ANNUAL"}) String billingCycle,
        @NotEmpty @Valid List<SelfServeQuoteLineRequest> lines) {
}
