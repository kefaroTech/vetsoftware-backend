package com.vetsoftware.app.quote.application.port.out;

/**
 * Asigna el consecutivo COT-AAAA-NNNNN que se le dice al cliente por telefono.
 */
public interface QuoteNumberPort {

    /**
     * Reserva el siguiente numero del ano de forma ATOMICA, incluido el primero:
     * ninguna implementacion puede leer un maximo y escribir despues, porque el ano
     * recien estrenado no tiene ninguna fila que bloquear y ahi es donde dos altas
     * simultaneas se llevarian el mismo numero.
     *
     * <p>
     * La reserva vive dentro de la transaccion de negocio que la pide: si el alta
     * de la cotizacion falla, el numero vuelve a estar libre y la serie no queda
     * con un hueco.
     */
    String next(int year);
}
