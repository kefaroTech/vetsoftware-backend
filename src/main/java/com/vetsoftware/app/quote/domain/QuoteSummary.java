package com.vetsoftware.app.quote.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyeccion de cabecera para los listados: SIN lineas y SIN respuestas.
 *
 * <p>
 * Existe por dos motivos concretos, y los dos son de ingenieria, no de gusto:
 *
 * <ul>
 * <li>Un listado paginado que ademas hace fetch de dos colecciones obliga a
 * Hibernate a paginar EN MEMORIA -trae la tabla entera y recorta despues-, que
 * es exactamente lo que la paginacion existe para evitar.
 * <li>Y no hace falta: los cuatro totales estan GUARDADOS en la cabecera. Poder
 * pintar un embudo comercial completo sin tocar una sola linea es la ventaja
 * practica de haberlos guardado en vez de calcularlos al vuelo.
 * </ul>
 *
 * <p>
 * Es una proyeccion de lectura y por eso no revalida el cuadre de totales: eso
 * lo hace {@link Quote}, que si tiene las lineas con las que compararlos.
 */
public record QuoteSummary(Long id, String quoteNumber, CompanyRef company, String prospectName,
        String prospectEmail, Long priceListId, BillingCycle billingCycle,
        BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, QuoteStatus status, LocalDate validUntil, int trialDays,
        LocalDateTime acceptedAt, LocalDateTime createdDate, boolean enabled) {
}
