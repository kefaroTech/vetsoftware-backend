package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * Un castigo sin firma nominal o sin motivo escrito.
 *
 * <p>
 * <b>Dar una deuda por incobrable es la unica operacion de esta tabla que hace
 * desaparecer dinero sin que entre nada a cambio</b>, y por eso es la unica que
 * exige nombre y explicacion. No se borra la deuda —el modelo lo prohibe— sino
 * que se agrega la fila que la da de baja, y las dos quedan; lo que hace
 * auditable esa fila es saber <b>quien</b> la autorizo y <b>por que</b>.
 *
 * <p>
 * <b>El autorizante lo pone el backend desde el principal, nunca el cuerpo de
 * la peticion.</b> Si viajara en el JSON, quien castiga la deuda elegiria a
 * quien atribuirsela, que es lo contrario de una firma.
 *
 * <p>
 * Espejo de {@code chk_bda_write_off_signature}.
 */
public class WriteOffSignatureRequiredException extends IllegalArgumentException {

    public WriteOffSignatureRequiredException(String detail) {
        super("Un castigo de cartera exige firma nominal de plataforma y motivo escrito: "
                + detail);
    }
}
