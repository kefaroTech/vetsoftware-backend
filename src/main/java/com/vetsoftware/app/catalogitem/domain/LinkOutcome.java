package com.vetsoftware.app.catalogitem.domain;

/**
 * Que hizo de verdad el alta de una fila puente: insertarla o revivir una que
 * estaba dada de baja.
 *
 * <p>
 * Las tres tablas puente del catalogo llevan borrado logico con
 * {@code @SQLRestriction} y una UNIQUE sobre sus claves foraneas, asi que una
 * fila retirada <strong>sigue ocupando la clave siendo invisible</strong>. Por
 * eso los tres casos de uso de alta reactivan en vez de insertar (ver
 * {@code LinkStateDto}). Lo que faltaba era contarlo: el servidor sabia cual de
 * las dos cosas habia pasado y no lo decia, asi que la consola tenia que
 * adivinarlo comparando {@code id} y {@code createdDate} -y fallaba cuando la
 * fila revivida resultaba ser la mas nueva-. Incidencia #465.
 *
 * <p>
 * <strong>Por que el estado HTTP sigue siendo 201 en los dos casos.</strong> Es
 * la misma decision que ya esta escrita en {@code QuoteController.create} para
 * el reintento idempotente: el codigo de estado describe el desenlace de la
 * peticion -«ahora ese vinculo existe»-, y devolver 200 en la rama de
 * reactivacion le haria creer al cliente que hizo otra cosa. Ademas obligaria a
 * cada consumidor a tratar dos codigos de exito para una sola accion, y el
 * contrato generado a declarar dos respuestas donde hay una. La distincion es
 * un <em>dato del recurso</em>, no un cambio de resultado, y por eso viaja en
 * el cuerpo tipada en vez de en el estado o en el texto de un mensaje.
 */
public enum LinkOutcome {

    /** La fila no existia y se inserto. */
    CREATED,

    /**
     * La fila existia dada de baja y se volvio a activar. Su {@code id} y su
     * {@code createdDate} son los de la primera vez, que es justamente lo que hacia
     * inutil deducirlo en el cliente.
     */
    REACTIVATED
}
