package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pasa la cotizacion de {@code DRAFT} a {@code SENT}.
 *
 * <p>
 * <strong>Es de la plataforma, por escrito.</strong> Enviar es el acto por el
 * que la plataforma publica su propia oferta y la vuelve vinculante: a partir
 * de ahi ya no se puede dar de baja y solo cabe aceptarla, rechazarla o dejarla
 * vencer. El cliente no se envia una oferta a si mismo; sus verbos sobre una
 * cotizacion son {@code AcceptQuoteUseCase} y {@code RejectQuoteUseCase}, que
 * si tienen rama de tenant.
 *
 * <p>
 * Ademas solo hay algo que enviar si antes hubo un {@code DRAFT}, y crear
 * borradores es {@link CreateQuoteUseCase}, tambien de plataforma: abrir el
 * envio al tenant no le daria ningun camino nuevo.
 */
public interface SendQuoteUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    QuoteDto execute(SendQuoteCommand command);
}
