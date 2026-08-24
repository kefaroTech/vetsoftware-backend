package com.vetsoftware.app.pricelist.application.dto;

/**
 * Estado de una fila <strong>ignorando el borrado lógico</strong>: su id y si
 * sigue activa.
 *
 * <p>
 * Lleva el mismo nombre que sus gemelos de {@code catalogitem} y
 * {@code configurator} a propósito: es el mismo patrón y conviene que se
 * reconozca de un vistazo. {@code price_lists} y {@code catalog_prices} llevan
 * {@code enabled} con {@code @SQLRestriction} y una UNIQUE que <em>no</em>
 * incluye esa columna —{@code uq_price_lists_code} y
 * {@code uq_catalog_prices_tier}—, así que una fila retirada sigue ocupando su
 * clave siendo invisible para la aplicación. Volver a darla de alta choca
 * contra una fila que nadie puede ver, y lo que sale por HTTP es un 409 con el
 * detalle genérico {@code Database constraint violation}.
 *
 * <p>
 * Los casos de uso de alta consultan primero este estado y
 * <strong>reactivan</strong> la fila en vez de insertar otra. Los adaptadores
 * lo resuelven con consulta nativa, que es la única forma de esquivar el
 * {@code @SQLRestriction} de la entidad.
 */
public record LinkStateDto(Long id, boolean enabled) {
}
