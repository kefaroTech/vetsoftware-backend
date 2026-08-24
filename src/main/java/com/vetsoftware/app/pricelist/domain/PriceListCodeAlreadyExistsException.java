package com.vetsoftware.app.pricelist.domain;

/**
 * El código de una lista de precios es único en base
 * ({@code uq_price_lists_code}) y es la clave de negocio que se cita en una
 * cotización y en un contrato.
 *
 * <p>
 * Se comprueba antes de insertar para que crear la tarifa del año siguiente con
 * un código ya usado —teclear {@code LISTA-2026} por inercia— dé un 409 que
 * nombra el campo culpable, y no el genérico
 * {@code Database constraint violation} que emite el handler cuando la
 * violación de índice llega sin mapeo: ese no dice cuál de los cinco campos
 * está mal, así que el formulario no puede señalarlo.
 *
 * <p>
 * La comprobación que la lanza <strong>ignora el borrado lógico</strong>,
 * porque la clave única también lo ignora.
 */
public class PriceListCodeAlreadyExistsException extends RuntimeException {
    public PriceListCodeAlreadyExistsException(String code) {
        super("PriceList code already exists: " + code);
    }
}
