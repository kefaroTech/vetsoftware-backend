package com.vetsoftware.app.quote.application.dto;

import java.math.BigDecimal;

/**
 * Una cotizacion cuya cabecera ya no cuadra con la suma de sus lineas activas.
 *
 * <p>
 * Es la salida de la vigilancia de la regla R5. Lleva las dos mitades —lo que
 * dice la cabecera y lo que suman las lineas— porque «no cuadra» sin decir por
 * cuanto no se puede triar: la diferencia distingue un centavo de redondeo de
 * una linea entera desaparecida.
 */
public record QuoteTotalsMismatchDto(Long quoteId, String quoteNumber, Long companyId,
        BigDecimal headerDiscountAmount, BigDecimal linesDiscountAmount, BigDecimal headerTaxAmount,
        BigDecimal linesTaxAmount, BigDecimal headerTotalAmount, BigDecimal linesTotalAmount) {
}
