package com.vetsoftware.app.quote.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuanto cuesta una seleccion, <strong>calculado por el servidor</strong>.
 *
 * <p>
 * Existe para que el front deje de multiplicar. La escalera de tramos es
 * acumulativa y no se publica —es la politica de descuento por volumen— asi que
 * un cliente que solo tiene el tramo de entrada solo puede extrapolar: quince
 * usuarios le salen 156.000 y el servidor cotiza 141.000. Publicar la escalera
 * entera arreglaria la cifra y regalaria la politica; devolver el importe ya
 * calculado arregla la cifra y no regala nada.
 *
 * <p>
 * <strong>Lo calcula el mismo codigo que congela una oferta real</strong>
 * ({@code QuoteLineFreezer}), asi que esto no es una segunda opinion: es la
 * misma cuenta, sin persistir nada.
 */
public record QuotePreviewDto(String currency, String billingCycle, List<QuotePreviewLineDto> lines,
        BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal taxAmount,
        BigDecimal totalAmount) {
}
