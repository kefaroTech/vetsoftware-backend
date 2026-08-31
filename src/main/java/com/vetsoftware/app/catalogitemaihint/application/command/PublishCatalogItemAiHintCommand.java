package com.vetsoftware.app.catalogitemaihint.application.command;

/**
 * Publica la primera pista de un articulo que no tiene ninguna.
 *
 * <p>
 * <strong>Sin {@code hintRevision}</strong>: el numero lo asigna el servicio a
 * partir del ultimo publicado para ese articulo, y no a partir de cuantas hay
 * vigentes. La diferencia importa cuando la pista se retiro y se vuelve a
 * publicar: si el numero se reiniciara en 1,
 * {@code uq_catalog_item_ai_hints_revision} abortaria el alta contra la
 * revision 1 que sigue en el historico.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: la tabla no la tiene, la pista
 * describe el catalogo global de plataforma y quien la lee es un prospecto
 * anonimo. No hay tenant que acotar.
 *
 * @param publishedBySystemUserId
 *            &#9940; <strong>Lo pone el controller desde la sesion
 *            ({@code authz.currentSystemUserId()}), nunca el cuerpo de la
 *            peticion.</strong> {@code PublishCatalogItemAiHintRequest} no
 *            tiene ni tendra este campo: un rastro de auditoria que escribe el
 *            auditado no es un rastro de auditoria, es un formulario. Es el
 *            mismo criterio que ya se aplico en
 *            {@code SuppressProposalDataCommand} y en la publicacion de
 *            tarifas. Y es obligatorio —no {@code currentSystemUserIdOrNull}—
 *            porque {@code published_by_system_user_id} es {@code NOT NULL} con
 *            clave foranea a {@code system_users}: sin firmante no hay fila.
 */
public record PublishCatalogItemAiHintCommand(Long catalogItemId, String hintText,
        Long publishedBySystemUserId) {

    public PublishCatalogItemAiHintCommand {
        if (catalogItemId == null) {
            throw new IllegalArgumentException("catalogItemId is required");
        }
        if (hintText == null || hintText.isBlank()) {
            throw new IllegalArgumentException("hintText is required");
        }
        if (publishedBySystemUserId == null) {
            throw new IllegalArgumentException("publishedBySystemUserId is required");
        }
    }
}
