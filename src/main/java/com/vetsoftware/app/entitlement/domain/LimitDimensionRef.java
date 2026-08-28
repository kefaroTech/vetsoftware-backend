package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * El eje visto desde el contador. Companion VO: el dominio de
 * {@code limitdimension} no se importa.
 *
 * <p>
 * Sustituye a la lista cerrada de cuatro unidades que habia aqui
 * ({@code CapacityUnit}: usuarios, sedes, terminales y almacenamiento). Ese
 * enum era lo que convertia «vender un limite nuevo» en un despliegue; con la
 * referencia al catalogo es insertar una fila.
 *
 * <p>
 * Trae el {@code measureKind} porque de el depende la invariante que la fila
 * tiene que poder comprobar sola --si lleva periodo real o centinela-- y una
 * restriccion no puede mirar otra tabla. Por eso el valor se copia en la
 * columna y esa copia va atada por clave foranea al eje real.
 *
 * <p>
 * <strong>Y trae {@code availableFrom}, que es la fecha en que nacio el
 * eje</strong> ({@code limit_dimensions.available_from}). Sin ella aqui, la
 * decision de D-74 no se puede tomar donde se toma: hay que distinguir «sin
 * fila porque no se vendio» de «sin fila porque el eje no existia cuando se
 * firmo», y esas dos ausencias tienen respuestas <em>opuestas</em> --techo cero
 * la primera, sin techo la segunda--. Ver {@link #postdates(LocalDate)}.
 */
public record LimitDimensionRef(Long id, String code, MeasureKind measureKind,
        LocalDate availableFrom) {

    public LimitDimensionRef {
        if (id == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("limit dimension code is required");
        if (measureKind == null)
            throw new IllegalArgumentException("limit dimension measure kind is required");
        // NOT NULL en la columna desde el changeset 300. Dejarlo entrar nulo aqui
        // convertiria la pregunta de D-74 en un NullPointerException a mitad del
        // camino de consumo, o --peor-- en un "no lo se" que alguien leeria como
        // "no lo limites".
        if (availableFrom == null)
            throw new IllegalArgumentException("limit dimension available from is required:"
                    + " without the date the axis was born, an absent counter row cannot be told"
                    + " apart from an axis that did not exist when the contract was signed");
    }

    /**
     * {@code true} si este eje nacio <strong>despues</strong> de que se firmara el
     * contrato (D-74).
     *
     * <p>
     * Quien firmo antes de que el eje existiera no acepto ese limite y no puede
     * quedar sujeto a el: para el, no tener fila significa <strong>sin
     * techo</strong>. Sin esta pregunta, añadir un eje de citas en abril deja
     * bloqueadas en el primer recalculo las cuarenta agendas de los contratos
     * firmados en enero --y el sintoma que ve soporte es «no puedo agendar», sin
     * ninguna relacion aparente con un cambio de catalogo--.
     *
     * <p>
     * La comparacion es estricta: firmar <em>el mismo dia</em> en que el eje entro
     * en vigor si sujeta al limite. Ese dia el eje ya existia y el techo formaba
     * parte de lo que se firmo.
     */
    public boolean postdates(LocalDate contractSignedOn) {
        if (contractSignedOn == null)
            // Sin contrato no hay firma anterior a la que ampararse. La regla vieja
            // --sin fila, techo cero-- sigue siendo la respuesta correcta aqui.
            return false;
        return availableFrom.isAfter(contractSignedOn);
    }
}
