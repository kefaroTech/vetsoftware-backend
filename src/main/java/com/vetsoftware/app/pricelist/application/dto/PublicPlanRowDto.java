package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Una fila plana del read model publico: un paquete del catalogo con su precio
 * de entrada en la tarifa vigente.
 *
 * <p>
 * Solo el <strong>tramo de entrada</strong> ({@code tier_min = 1}). La escalera
 * completa —{@code tierMax} y los tramos siguientes— es la politica de
 * descuento por volumen, y publicarla entera es publicarla.
 *
 * <p>
 * {@code monthlyFromAmount} o {@code annualFromAmount} pueden venir nulos: un
 * paquete puede estar tarifado en un solo ciclo. Lo que no puede es venir sin
 * ninguno de los dos; esa fila la descarta la consulta.
 */
public record PublicPlanRowDto(String code, String name, String tagline,
        BigDecimal monthlyFromAmount, BigDecimal annualFromAmount, BigDecimal setupAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment) {
}
