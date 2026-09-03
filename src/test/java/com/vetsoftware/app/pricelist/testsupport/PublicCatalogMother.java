package com.vetsoftware.app.pricelist.testsupport;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogAreaRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackRowDto;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Fixtures del read model del catalogo <em>contratable</em>: las filas planas
 * que devuelve {@code PublicCatalogQueryPort} antes de que
 * {@code GetPublicCatalogService} las reparta por naturaleza.
 *
 * <p>
 * Aparte de {@link PublicPlanMother} porque son dos read models distintos
 * —aquel responde «que trae este paquete», este «cuanto cuesta esta pieza
 * suelta»— y comparten solo la cabecera del paquete, que es el mismo record a
 * proposito.
 */
public final class PublicCatalogMother {

    /** El nucleo: {@code structural_minimum = TRUE}. Ver {@link #nucleo()}. */
    public static final String CORE = "CORE";

    public static final String MODULO = "SURGERY";
    public static final String CONTADOR = "CAPACITY_USER";
    public static final String CARGO_UNICO = "ONBOARDING";
    public static final String PAQUETE = "PACK_CLINIC";
    public static final String AREA = "PATIENT_CARE";

    private PublicCatalogMother() {
    }

    /**
     * <b>El minimo estructural.</b> {@code mandatory = true} es
     * {@code catalog_items.structural_minimum}, la misma columna con la que
     * {@code PlatformCatalogTemplateJpaRepository} monta el contrato inicial de
     * toda empresa: sin esa fila el alta falla entera. No es «recomendado».
     *
     * <p>
     * <b>Sin area, aunque sea un {@code MODULE}.</b> El changeset 399 deja
     * {@code CORE.area_code} en {@code NULL} a proposito —se pinta en una fila fija
     * sobre las cabeceras plegables, no dentro de ninguna— y 400 lo bendice
     * excluyendolo de su preCondition. Una fixture con area describia un mundo que
     * la semilla no produce.
     */
    public static PublicCatalogItemRowDto nucleo() {
        return new PublicCatalogItemRowDto(CORE, "Nucleo: clientes y mascotas",
                "Lo que toda clinica necesita", "MODULE", true, null, null,
                new BigDecimal("49000.00"), new BigDecimal("490000.00"), null, null,
                new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, true, null,
                "Nucleo");
    }

    /**
     * Un modulo suelto con precio en los dos ciclos y prueba concedida.
     *
     * <p>
     * El anual <b>no</b> es el mensual por doce (456.000) ni por diez (380.000):
     * 350.000 no es multiplo de 38.000. Una fixture donde lo fuera dejaria pasar un
     * servicio que extrapolara en vez de leer la columna, que es exactamente el
     * defecto que ya costo un arreglo en el catalogo de planes.
     */
    public static PublicCatalogItemRowDto modulo() {
        return new PublicCatalogItemRowDto(MODULO, "Cirugia", "Partes quirurgicos y protocolos",
                "MODULE", false, null, 30, new BigDecimal("38000.00"), new BigDecimal("350000.00"),
                null, null, new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                true, "HOSPITAL", "Cirugia");
    }

    /**
     * Modulo tarifado <b>solo al mes</b>. El importe anual nulo no es un hueco: es
     * la misma respuesta que dara el gate de la contratacion, que exige precio de
     * entrada en el ciclo pedido con un {@code JOIN} interno.
     */
    public static PublicCatalogItemRowDto moduloSoloMensual() {
        return new PublicCatalogItemRowDto("GROOMING", "Peluqueria", null, "MODULE", false, null,
                null, new BigDecimal("29000.00"), null, null, null, new BigDecimal("0.00"),
                new BigDecimal("19.00"), TaxTreatment.TAXED, true, AREA, null);
    }

    /** Contador con unidades incluidas distintas en cada ciclo. */
    public static PublicCatalogItemRowDto contador() {
        return new PublicCatalogItemRowDto(CONTADOR, "Usuario adicional", null, "CAPACITY", true,
                "USER", null, new BigDecimal("15000.00"), new BigDecimal("145000.00"), 3, 5,
                new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, true, null,
                null);
    }

    /**
     * Cargo unico, copiado de {@code DATA_MIGRATION} tal como lo tarifa la semilla
     * 310: <b>cero en los dos ciclos y 450.000 en {@code setup_amount}</b>.
     *
     * <p>
     * <b>Esa forma es el punto de la fixture.</b> Un cargo unico no lleva su precio
     * en {@code unit_amount} sino en el cargo de implantacion, asi que un catalogo
     * que publicara solo los importes por ciclo anunciaria la migracion de datos a
     * cero — gratis— sin que nada fallara. Con la fixture puesta al valor «bonito»
     * (900.000 en el importe mensual) el defecto no se veia.
     *
     * <p>
     * {@code selfServiceEligible = false} porque no cuelga de ningun paquete: la
     * autocontratacion lo rechazaria, asi que se publica su precio de lista pero no
     * se ofrece como linea.
     */
    public static PublicCatalogItemRowDto cargoUnico() {
        return new PublicCatalogItemRowDto(CARGO_UNICO, "Migracion de datos",
                "Traemos tu historico", "ONE_TIME", false, null, null, new BigDecimal("0.00"),
                new BigDecimal("0.00"), 0, 0, new BigDecimal("450000.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED, false, null, null);
    }

    public static PublicCatalogPackComponentRowDto componente(String componentCode) {
        return new PublicCatalogPackComponentRowDto(PAQUETE, componentCode);
    }

    public static PublicCatalogPackRowDto paquete(String code, boolean recommended) {
        return new PublicCatalogPackRowDto(code, "Pack " + code, "Para una clinica que empieza",
                new BigDecimal("89000.00"), new BigDecimal("890000.00"),
                new BigDecimal("150000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                recommended);
    }

    public static PublicCatalogAreaRowDto area(String code) {
        return new PublicCatalogAreaRowDto(code, "Area " + code);
    }
}
