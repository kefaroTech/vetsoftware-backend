package com.vetsoftware.app.taxreturn.domain;

/**
 * Una declaracion no se corrige a si misma.
 *
 * <p>
 * <strong>Es LA invariante de esta rodaja que la base no puede garantizar, y
 * esta escrita como tal en la especificacion (§7.2).</strong> El manual de
 * MySQL prohibe referenciar una columna {@code AUTO_INCREMENT} dentro de un
 * {@code CHECK}, asi que {@code CHECK (corrects_return_id <> id)} <b>no se
 * puede escribir</b>. {@code fk_tax_returns_corrects} solo garantiza que la
 * declaracion apuntada existe; que no sea ella misma no lo mira nadie mas que
 * este dominio.
 *
 * <p>
 * <strong>Y hoy se cumple ademas de forma vacia por construccion, lo que hace
 * facil borrarla sin notar nada.</strong> En el {@code INSERT} el id todavia no
 * existe, asi que una fila recien creada no puede apuntarse a si misma; el
 * unico camino real seria un {@code UPDATE} que escribiera
 * {@code corrects_return_id = id}, y esta rodaja no ofrece ninguno —
 * {@code correctsReturnId} es inmutable desde que la fila nace—. Esta
 * comprobacion es la red para el dia que alguien añada ese camino: sin ella, la
 * cadena de correcciones se cierra sobre si misma y cualquier recorrido del
 * historico entra en bucle infinito.
 */
public class TaxReturnCannotCorrectItselfException extends RuntimeException {

    public TaxReturnCannotCorrectItselfException(Long id) {
        super("Tax return " + id + " cannot correct itself");
    }
}
