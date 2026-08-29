package com.vetsoftware.app.quote.application.command;

import java.util.List;

/**
 * Lo que el prospecto quiere que le tarifen.
 *
 * <p>
 * <strong>Sin empresa y sin ningun termino economico</strong>, igual que
 * {@link SelfServeQuoteCommand}: aqui no se firma nada, se pregunta un precio.
 * Ni tarifa, ni descuento, ni vigencia — y no validados a cero, sino
 * inexpresables.
 */
public record PreviewQuoteCommand(String billingCycle, List<SelfServeQuoteLineCommand> lines) {
}
