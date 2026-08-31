package com.vetsoftware.app.catalogitemaihint.application.dto;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import java.time.LocalDateTime;

/**
 * Una revision de pista tal como la ve la consola de plataforma.
 *
 * <p>
 * Lleva {@code hintText} completo porque el caso de uso central es <em>leer lo
 * que se le esta diciendo al modelo</em>, y una pista que no se puede exhibir
 * no sirve para decidir si propone mal.
 *
 * <p>
 * <strong>{@code catalogItemCode} y {@code catalogItemName} pueden venir
 * nulos.</strong> Se resuelven por {@code CatalogItemQueryPort}, que solo
 * devuelve articulos habilitados: la pista de un articulo borrado logicamente
 * sigue siendo una fila legitima del historial, y esconderla seria peor que
 * servirla sin nombre. Un articulo meramente {@code DEPRECATED} <b>si</b> trae
 * codigo y nombre en el listado —el criterio del pintado no es el de la guarda
 * de publicacion, ver el Javadoc del puerto—.
 *
 * <p>
 * <strong>{@code supersededBySystemUserId} tambien puede venir nulo, y ese nulo
 * es informacion.</strong> Significa <em>«no consta»</em>, y se da en dos casos
 * que la pantalla tiene que poder distinguir de «lo retiro fulano»: la revision
 * <b>vigente</b> —que no la ha retirado nadie— y toda revision sucedida
 * <b>antes del changeset 393</b>, cuyo actor real no se escribio nunca y no se
 * puede reconstruir. Inventarle un firmante seria convertir una laguna conocida
 * en un dato falso indistinguible de una firma real.
 *
 * @param publishedBySystemUserId
 *            quien escribio este texto.
 * @param supersededBySystemUserId
 *            quien decidio que dejara de regir. Va como <b>identificador y no
 *            como nombre</b>, igual que su gemelo de publicacion: es el
 *            criterio de todas las respuestas del backend —ninguna expone el
 *            nombre de una cuenta de plataforma— y evita que esta rodaja tenga
 *            que alcanzar la feature de usuarios de sistema para resolver un
 *            dato derivado y mutable que la consola ya resuelve para el otro
 *            firmante.
 * @param current
 *            derivado de {@code supersededAt == null}. Viaja resuelto para que
 *            el front no tenga que reimplementar la regla de vigencia —y
 *            equivocarse— en dos repositorios distintos.
 */
public record CatalogItemAiHintDto(Long id, Long catalogItemId, String catalogItemCode,
        String catalogItemName, int hintRevision, String hintText, LocalDateTime publishedAt,
        Long publishedBySystemUserId, LocalDateTime supersededAt, Long supersededBySystemUserId,
        boolean current, LocalDateTime createdDate) {

    public static CatalogItemAiHintDto from(CatalogItemAiHint hint, CatalogItemRef articulo) {
        return new CatalogItemAiHintDto(hint.getId(), hint.getCatalogItemId(),
                articulo == null ? null : articulo.code(),
                articulo == null ? null : articulo.name(), hint.getHintRevision(),
                hint.getHintText(), hint.getPublishedAt(), hint.getPublishedBySystemUserId(),
                hint.getSupersededAt(), hint.getSupersededBySystemUserId(), hint.isCurrent(),
                hint.getCreatedDate());
    }
}
