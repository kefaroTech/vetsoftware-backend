package com.vetsoftware.app.pricelist.application.dto;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Una fila plana del catalogo contratable: un articulo <em>suelto</em>
 * —{@code MODULE}, {@code CAPACITY} o {@code ONE_TIME}— con su precio de
 * entrada en los dos ciclos de la tarifa vigente.
 *
 * <p>
 * <strong>Es la pieza que {@code GET /plans} no tiene.</strong> Alli un modulo
 * sale como {@code PublicPlanIncludedDto} —codigo, nombre y dias de prueba— y
 * <em>sin precio</em>, porque alli el precio es el del paquete que lo contiene.
 * Un configurador «solo compre lo que necesite» necesita el precio del articulo
 * por si mismo, y esa cifra existe en {@code catalog_prices} desde el changeset
 * 310: lo que faltaba era publicarla.
 *
 * <p>
 * <strong>Los dos importes pueden ser nulos, y el nulo es la
 * respuesta.</strong> Significa «este articulo no se vende suelto en ese
 * ciclo», que es exactamente lo que va a decidir la contratacion: el
 * {@code JOIN} interno de {@code JpaPublishedCatalogItemQueryPort} exige precio
 * de entrada ({@code tier_min = 1}) en el ciclo pedido. Publicar un cero o
 * extrapolar el otro ciclo pondria en la portada un numero que el gate rechaza
 * despues, que es el defecto que ya costo un arreglo en {@code SQL_COMPONENTS}.
 *
 * @param itemType
 *            el discriminante en crudo ({@code MODULE}, {@code CAPACITY},
 *            {@code ONE_TIME}). Viaja como texto y no como el {@code ItemType}
 *            de {@code catalogitem} porque {@code pricelist} no importa el
 *            dominio de otra feature; el reparto en las tres listas de la
 *            respuesta lo hace el servicio con este campo.
 * @param mandatory
 *            {@code catalog_items.structural_minimum}. No es «recomendado»: es
 *            el minimo estructural, el conjunto sin el cual una empresa no
 *            puede existir. Ver {@code PublicCatalogItemDto#mandatory()}.
 * @param trialDays
 *            ya filtrado por politica —nulo salvo {@code trial_eligibility =
 *            'ELIGIBLE'}—, por lo mismo que en
 *            {@link PublicPlanComponentRowDto}: leer {@code default_trial_days}
 *            sin mirar la elegibilidad ata la promesa publica a una sola mitad
 *            de un arco exclusivo.
 * @param monthlyIncludedQuantity
 *            lo que trae el propio tramo de entrada mensual
 *            ({@code catalog_prices.included_quantity}), no lo que trae un
 *            paquete. Es la cifra que resta {@code TieredPrice} antes de
 *            repartir, asi que es la que hace cuadrar el precio anunciado con
 *            el cotizado. Nulo si no hay tramo mensual.
 * @param annualIncludedQuantity
 *            lo mismo para {@code ANNUAL}.
 * @param setupAmount
 *            {@code catalog_prices.setup_amount}, y <strong>en un
 *            {@code ONE_TIME} es TODO el precio</strong>. La semilla 310 pone
 *            {@code DATA_MIGRATION} a {@code unit_amount = 0.00} con
 *            {@code setup_amount = 450000.00}: publicar solo los importes por
 *            ciclo lo anunciaria como gratis. Es la columna que convierte un
 *            cero enganoso en la cifra real, y por eso no se puede omitir. En
 *            un modulo o un contador vale {@code 0.00} en todo el catalogo
 *            sembrado, que es un importe de verdad —«no hay cargo de
 *            implantacion»— y no una ausencia de dato.
 * @param selfServiceEligible
 *            si el articulo cuelga de algun paquete {@code ACTIVE} publicado o
 *            lleva {@code catalog_items.self_service}. <strong>Es el predicado
 *            del gate, proyectado</strong>: junto con «tiene importe en el
 *            ciclo pedido» reproduce exactamente el {@code WHERE} de
 *            {@code JpaPublishedCatalogItemQueryPort.SQL_PUBLISHED_ID_BY_CODE}.
 *            Se publica en vez de filtrar por el para que la portada pueda
 *            mostrar el cargo unico —que existe y tiene precio de lista— sin
 *            ofrecerlo como linea de autoservicio, que es lo que la
 *            contratacion rechazaria.
 * @param areaCode
 *            {@code catalog_items.area_code}. Nulo fuera de los {@code MODULE}
 *            —{@code chk_catalog_items_area} lo prohibe— y tambien en
 *            {@code CORE}, que es {@code MODULE} y aun asi lo tiene nulo: el
 *            nucleo se pinta en una fila fija sobre las cabeceras plegables y
 *            no dentro de ninguna (changeset 399, issue #711).
 * @param shortLabel
 *            rotulo de casilla, no la frase de escaparate de
 *            {@code shortDescription}. Nulable: la caida a {@code name} es
 *            decision de quien pinta.
 */
public record PublicCatalogItemRowDto(String code, String name, String shortDescription,
        String itemType, boolean mandatory, String capacityUnit, Integer trialDays,
        BigDecimal monthlyAmount, BigDecimal annualAmount, Integer monthlyIncludedQuantity,
        Integer annualIncludedQuantity, BigDecimal setupAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment, boolean selfServiceEligible, String areaCode,
        String shortLabel) {

    /** Un contador que se compra por unidades. */
    public boolean esCapacidad() {
        return "CAPACITY".equals(itemType);
    }

    /** Un cargo unico: implantacion, migracion, capacitacion. */
    public boolean esCargoUnico() {
        return "ONE_TIME".equals(itemType);
    }

    /** Una funcionalidad que se enciende. */
    public boolean esModulo() {
        return "MODULE".equals(itemType);
    }
}
