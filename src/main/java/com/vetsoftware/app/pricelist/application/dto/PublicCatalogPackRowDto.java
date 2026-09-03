package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Una fila plana de paquete del catalogo contratable: la cabecera de
 * {@link PublicPlanRowDto} mas la marca comercial de combinacion recomendada.
 */
public record PublicCatalogPackRowDto(String code, String name, String tagline,
        BigDecimal monthlyFromAmount, BigDecimal annualFromAmount, BigDecimal setupAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment, boolean recommended) {
}
