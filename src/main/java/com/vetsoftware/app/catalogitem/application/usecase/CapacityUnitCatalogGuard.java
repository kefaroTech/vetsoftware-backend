package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.port.out.LimitDimensionQueryPort;

/**
 * La comprobacion que salio del constructor de {@code CatalogItem} cuando el
 * changeset 333 abrio {@code capacity_unit} al catalogo de ejes (#655).
 *
 * <p>
 * <strong>Por que no esta en el dominio.</strong> Que el codigo del eje exista
 * no es una invariante de la entidad: es una pregunta al catalogo, y una
 * consulta no cabe en un constructor —el dominio no conoce puertos—. Lo que si
 * se queda alli es la mitad que se comprueba sin mirar nada (una CAPACITY exige
 * unidad, el resto la prohibe), y en la base la comprobacion la repite
 * {@code fk_catalog_items_capacity_unit}. El caso de uso existe para que el
 * cliente reciba un {@code 400} que nombra el codigo que falta, en vez del
 * {@code 500} de una violacion de clave foranea.
 *
 * <p>
 * <strong>Por que es una clase aparte y no un metodo privado repetido.</strong>
 * La misma comprobacion la necesitan el alta y la edicion, y son dos servicios
 * distintos por la regla de un caso de uso por servicio. Duplicarla es como
 * empiezan las dos validaciones que divergen: una acepta un codigo que la otra
 * rechaza, y el articulo queda editable pero no creable.
 */
final class CapacityUnitCatalogGuard {

    private CapacityUnitCatalogGuard() {
    }

    /**
     * Falla si {@code capacityUnit} no corresponde a ningun eje sembrado.
     *
     * <p>
     * Un valor nulo pasa sin consultar: significa que el articulo no es de tipo
     * {@code CAPACITY}, y de eso ya se ocupa el constructor de la entidad. Que la
     * unidad sea obligatoria en una {@code CAPACITY} tampoco se decide aqui.
     *
     * <p>
     * El mensaje nombra el codigo y dice como arreglarlo, porque la respuesta casi
     * siempre es sembrar la fila: la promesa de la capa J es que vender un eje
     * nuevo sea insertar en {@code limit_dimensions} y no desplegar, y un «valor
     * invalido» generico esconde justamente esa salida.
     */
    static void requireKnownAxis(LimitDimensionQueryPort limitDimensionQueryPort,
            String capacityUnit) {
        if (capacityUnit == null)
            return;
        if (limitDimensionQueryPort.findByCode(capacityUnit).isEmpty())
            throw new IllegalArgumentException("capacityUnit '" + capacityUnit
                    + "' is not a known limit dimension: seed a limit_dimensions row with that"
                    + " code before selling capacity of that axis. Codes are case sensitive.");
    }
}
