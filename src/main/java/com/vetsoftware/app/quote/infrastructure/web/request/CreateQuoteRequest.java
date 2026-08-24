package com.vetsoftware.app.quote.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Alta de cotizacion.
 *
 * <p>
 * <b>No lleva companyId</b>, y no puede llevarlo: la empresa la deriva el
 * controller del principal autenticado. Aceptarla en el cuerpo permitiria
 * cotizarle a otra clinica, y es lo que la regla
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} rompe el build por hacer.
 *
 * <p>
 * El {@code @Valid} de las listas no es decorativo: sin el, las restricciones
 * de {@link QuoteLineRequest} estan escritas y no se evaluan nunca.
 *
 * @param clientRequestId
 *            llave de idempotencia que genera el cliente. Si el navegador
 *            reenvia la peticion, la segunda recibe la misma cotizacion en vez
 *            de crear otra.
 */
public record CreateQuoteRequest(@NotBlank @Size(max = 64) String clientRequestId,
        @Size(max = 150) String prospectName, @Email @Size(max = 120) String prospectEmail,
        @Size(max = 50) String prospectDocument, @Size(max = 30) String prospectPhone,
        @NotNull Long priceListId, @NotBlank @Size(max = 20) String billingCycle,
        @NotNull LocalDate validUntil, @PositiveOrZero int trialDays,
        @NotEmpty @Valid List<QuoteLineRequest> lines, @Valid List<QuoteAnswerRequest> answers) {
}
