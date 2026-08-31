package com.vetsoftware.app.catalogitemaihint.application.command;

/**
 * Corrige la pista de un articulo: marca la vigente como reemplazada y publica
 * la revision siguiente.
 *
 * <p>
 * &#9940; <strong>No es un update, y esa es toda la diferencia.</strong> La
 * tabla esta disenada para historial: sobrescribir {@code hint_text} dejaria
 * sin explicacion cualquier propuesta ya generada, porque el texto con el que
 * se genero habria dejado de existir. Lo que este command produce son
 * <em>dos</em> escrituras —el cierre de la vigente y el alta de la siguiente—,
 * y por eso su servicio es {@code @Transactional}: a medias, el articulo se
 * quedaria sin pista vigente y el prompt dejaria de proponerlo sin que nadie lo
 * hubiera decidido.
 *
 * @param revisedBySystemUserId
 *            &#9940; Sale de la sesion, nunca del cuerpo. Es el firmante de la
 *            revision <em>nueva</em>: la anterior conserva el suyo, que es lo
 *            que permite leer en el historial quien escribio cada texto.
 */
public record ReviseCatalogItemAiHintCommand(Long catalogItemId, String hintText,
        Long revisedBySystemUserId) {

    public ReviseCatalogItemAiHintCommand {
        if (catalogItemId == null) {
            throw new IllegalArgumentException("catalogItemId is required");
        }
        if (hintText == null || hintText.isBlank()) {
            throw new IllegalArgumentException("hintText is required");
        }
        if (revisedBySystemUserId == null) {
            throw new IllegalArgumentException("revisedBySystemUserId is required");
        }
    }
}
