package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Un articulo contratable suelto, ya listo para la respuesta: un modulo o un
 * cargo unico.
 *
 * <p>
 * <strong>{@code mandatory} sale del modelo, no de una opinion.</strong> Es
 * {@code catalog_items.is_core}, y esa columna significa «forma parte del
 * minimo estructural». Que sea cierto para {@code CORE} no es una convencion
 * comercial:
 * {@code PlatformCatalogTemplateJpaRepository.findInitialContractTemplate}
 * monta el contrato inicial de toda empresa con un {@code JOIN}
 * <em>interno</em> sobre {@code ci.code = 'CORE' AND ci.is_core = TRUE}, y si
 * esa fila falta el alta entera falla con
 * {@code PlatformCatalogNotConfiguredException}. Ademas los tres paquetes
 * sembrados lo llevan dentro. Dicho de otro modo: no existe empresa sin nucleo,
 * asi que el front no debe pintarlo como una casilla que se pueda desmarcar.
 *
 * <p>
 * <strong>Y por eso mismo el nucleo no se vuelve a comprar.</strong> Lo concede
 * el alta, no la cotizacion. Publicarlo con su precio sirve para que el
 * configurador explique de que se compone la cuota, no para que lo anada a la
 * cesta.
 *
 * <p>
 * <strong>{@code setupAmount} no es un adorno en un {@code ONE_TIME}: es su
 * precio entero.</strong> La semilla 310 tarifa {@code DATA_MIGRATION} con
 * {@code unit_amount = 0.00} en los dos ciclos y {@code setup_amount =
 * 450000.00}. Un catalogo que publicara solo los importes por ciclo anunciaria
 * la migracion de datos como gratuita — un cero que se lee como una promesa.
 *
 * @param selfServiceEligible
 *            si la autocontratacion lo aceptaria como linea. Ver
 *            {@link PublicCatalogItemRowDto#selfServiceEligible()}: es el
 *            predicado del gate, no una etiqueta editorial. Hoy sale
 *            {@code false} en los {@code ONE_TIME} —implantacion y migracion
 *            son cargos negociados— y {@code true} en los modulos, que cuelgan
 *            todos de algun paquete publicado.
 */
public record PublicCatalogItemDto(String code, String name, String description, boolean mandatory,
        Integer trialDays, BigDecimal monthlyAmount, BigDecimal annualAmount,
        BigDecimal setupAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        boolean selfServiceEligible) {

    /** Proyecta la fila plana del read model a la forma que sale por HTTP. */
    public static PublicCatalogItemDto from(PublicCatalogItemRowDto row) {
        return new PublicCatalogItemDto(row.code(), row.name(), row.shortDescription(),
                row.mandatory(), row.trialDays(), row.monthlyAmount(), row.annualAmount(),
                row.setupAmount(), row.taxRate(), row.taxTreatment(), row.selfServiceEligible());
    }
}
