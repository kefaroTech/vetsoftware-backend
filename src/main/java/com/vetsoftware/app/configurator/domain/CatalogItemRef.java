package com.vetsoftware.app.configurator.domain;

/**
 * El articulo del catalogo tal como lo necesita el configurador: su id interno,
 * su <strong>rotulo publico</strong>, y —si es un contador— el eje que consume
 * y si pertenece al minimo estructural.
 *
 * <p>
 * <strong>El rotulo existe porque el id no puede salir.</strong> Hasta hoy
 * {@code /configurator/resolve} devolvia {@code catalogItemId} —la clave
 * primaria, secuencial— en una respuesta anonima. Eso es un oraculo de
 * enumeracion: probando enteros se recorre el catalogo entero, incluidos los
 * articulos en borrador y los retirados que {@code GET /catalog-items} cierra a
 * {@code SYSTEM} precisamente para no ensenar. Es el mismo defecto que ya se
 * retiro de la autocontratacion, y la misma razon por la que
 * {@code PublicPlanResponse} no publica ni un id.
 *
 * <p>
 * <strong>El eje y {@code core} existen para poder restar lo ya
 * incluido.</strong> Un {@code EXTRA_USER} es {@code CAPACITY} sobre el eje
 * {@code USER} y {@code is_core = FALSE}; el techo que ya trae el contrato lo
 * aporta {@code CAPACITY_USER}, que es {@code is_core = TRUE} sobre el
 * <em>mismo</em> eje. Ese eje compartido es el unico vinculo que el modelo
 * tiene entre los dos —no hay FK ni {@code relation_type}— y por eso viaja
 * aqui.
 *
 * @param capacityUnit
 *            codigo del eje ({@code limit_dimensions.code}), o nulo si el
 *            articulo no es un contador. No nulo <em>si y solo si</em> el
 *            articulo es {@code CAPACITY}, que es la invariante que
 *            {@code chk_catalog_items_capacity_unit} impone en el esquema.
 */
public record CatalogItemRef(Long id, String code, String capacityUnit, boolean core) {

    public CatalogItemRef {
        if (id == null) {
            throw new IllegalArgumentException("catalog item id is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("catalog item code is required");
        }
    }

    /** Un contador que se compra por unidades sobre un eje. */
    public boolean esContador() {
        return capacityUnit != null && !capacityUnit.isBlank();
    }

    /**
     * Un contador que se factura por encima del techo, frente al que
     * <em>aporta</em> el techo ({@code is_core}). Solo a este se le resta lo ya
     * incluido: restarle el techo al articulo que lo concede lo dejaria siempre en
     * cero.
     */
    public boolean esUnidadFacturable() {
        return esContador() && !core;
    }
}
