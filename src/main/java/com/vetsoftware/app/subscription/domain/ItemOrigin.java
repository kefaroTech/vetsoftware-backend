package com.vetsoftware.app.subscription.domain;

/**
 * Por que nacio esta linea del contrato. {@code chk_subscription_items_origin}.
 *
 * <p>
 * {@code QUANTITY_CHANGE} y {@code REMOVAL} existen porque en este modelo
 * <strong>nada se edita</strong>: subir la cantidad es cerrar la linea vieja y
 * abrir otra, y dar de baja es poner {@code effective_to}. El origen es lo que
 * deja leer la pelicula despues.
 */
public enum ItemOrigin {
    INITIAL, ADDON, QUANTITY_CHANGE, REMOVAL, MIGRATION
}
