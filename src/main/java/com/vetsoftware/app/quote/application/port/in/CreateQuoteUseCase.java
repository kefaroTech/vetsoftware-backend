package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alta de cotizacion, idempotente por clientRequestId.
 *
 * <p>
 * Los terminos comerciales crudos (tarifa, descuento, vigencia y prueba) solo
 * los fija la plataforma. Un flujo tenant futuro debe resolverlos en servidor.
 *
 * <p>
 * <strong>Y eso no es una aspiracion: es el motivo del gate.</strong>
 * {@code QuoteLineRequest.discountPercent} viaja en el cuerpo y
 * {@code CreateQuoteService.freezeLines} lo pasa a {@code QuoteLine.freeze} sin
 * mediar tarifa —el precio y el IVA si se leen del catalogo, el descuento no—,
 * y {@code validUntil} y {@code trialDays} tambien los elige quien llama. Un
 * tenant con este puerto abierto se cotizaria al 100 % de descuento con la
 * prueba que quisiera. Cerrarlo no es lo que impide el autoservicio: lo que
 * falta es resolver el descuento y la vigencia en servidor.
 *
 * <p>
 * Lo que el cliente si puede hacer con una cotizacion ya emitida es aceptarla o
 * rechazarla, y esos dos puertos si tienen su rama de tenant.
 */
public interface CreateQuoteUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    QuoteDto execute(CreateQuoteCommand command);
}
