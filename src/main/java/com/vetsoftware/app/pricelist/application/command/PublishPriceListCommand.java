package com.vetsoftware.app.pricelist.application.command;

/**
 * Publicar es firmar: {@code chk_price_lists_published} exige que una lista
 * fuera de DRAFT tenga quien la publico y cuando.
 *
 * <p>
 * El {@code publishedBySystemUserId} <strong>no viaja en el cuerpo de la
 * peticion</strong>. Lo pone el controller con
 * {@code authz.currentSystemUserId()}, por el mismo motivo por el que
 * {@code companyId} tampoco viaja: quien escribe el campo elegiria lo que la
 * columna existe para probar, y un rastro de auditoria que el auditado escribe
 * no es un rastro de auditoria.
 */
public record PublishPriceListCommand(Long id, Long publishedBySystemUserId) {
}
