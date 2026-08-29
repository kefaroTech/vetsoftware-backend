package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Un contador comprado suelto: cuantas unidades trae su tramo de entrada y a
 * como sale la unidad en cada ciclo.
 *
 * <p>
 * <strong>No confundir con {@link PublicPlanCapacityDto}</strong>, que responde
 * a otra pregunta. Aquel dice «cuantas unidades trae <em>este paquete</em>»
 * —{@code bundle_components.quantity}— y a como sale la unidad adicional. Este
 * dice «cuantas trae el <em>propio articulo</em> en su tramo de entrada»
 * —{@code catalog_prices.included_quantity}— para quien no compra ningun
 * paquete. Son dos cifras distintas del mismo contador y por eso son dos
 * records: fusionarlos obligaria a que uno de los dos campos mintiera segun el
 * contexto.
 *
 * <p>
 * {@code monthlyIncludedQuantity} y {@code annualIncludedQuantity} van por
 * separado porque {@code included_quantity} es columna de la <em>fila de
 * precio</em>, y hay una fila por ciclo. Nada obliga a que coincidan.
 */
public record PublicCatalogCapacityDto(String code, String name, String description,
        boolean mandatory, String unit, Integer monthlyIncludedQuantity,
        Integer annualIncludedQuantity, BigDecimal monthlyUnitAmount, BigDecimal annualUnitAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment, boolean selfServiceEligible) {

    /** Proyecta la fila plana del read model a la forma que sale por HTTP. */
    public static PublicCatalogCapacityDto from(PublicCatalogItemRowDto row) {
        return new PublicCatalogCapacityDto(row.code(), row.name(), row.shortDescription(),
                row.mandatory(), row.capacityUnit(), row.monthlyIncludedQuantity(),
                row.annualIncludedQuantity(), row.monthlyAmount(), row.annualAmount(),
                row.taxRate(), row.taxTreatment(), row.selfServiceEligible());
    }
}
