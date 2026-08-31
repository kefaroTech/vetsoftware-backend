package com.vetsoftware.app.catalogitemaihint.infrastructure.web.response;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Una revision de pista tal como sale por HTTP.
 *
 * <p>
 * {@code publishedBySystemUserId} viaja al front —a diferencia de lo que hace
 * {@code PublicLegalDocumentResponse}— porque aqui no hay ruta anonima: todos
 * los endpoints de esta feature exigen una cuenta de plataforma, y quien
 * administra el catalogo necesita saber quien escribio cada texto.
 *
 * <p>
 * <strong>{@code supersededBySystemUserId} va como identificador, no como
 * nombre</strong>, exactamente igual que {@code publishedBySystemUserId} justo
 * encima. Es el criterio de todo el backend —ninguna respuesta del repositorio
 * expone el nombre de una cuenta de plataforma— y aqui ademas evita que esta
 * feature tenga que alcanzar {@code system_users} para servir un dato derivado
 * y mutable que la consola ya resuelve para el otro firmante. Dos firmas del
 * mismo esquema con formas distintas serian peor contrato que dos ids.
 *
 * <p>
 * &#9888; <strong>Es nulable y su nulo significa «no consta», no «falta el
 * dato».</strong> Se da en la revision vigente —que no la ha retirado nadie— y
 * en toda revision sucedida antes del changeset 393, cuyo actor no quedo
 * escrito en ningun sitio. La pantalla de administracion tiene que poder
 * distinguir ese caso de «la retiro fulano», y por eso el campo NO va
 * {@code REQUIRED}: marcarlo obligatorio obligaria a inventar un firmante para
 * las filas historicas, que es convertir una laguna conocida en un dato falso.
 *
 * <p>
 * {@code catalogItemCode} y {@code catalogItemName} son opcionales por
 * construccion: la pista de un articulo retirado sigue en el historial y se
 * sirve sin ellos.
 */
public record CatalogItemAiHintResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(example = "GROOMING", description = "Nulo si el articulo se retiro del catalogo") String catalogItemCode,
        @Schema(description = "Nulo si el articulo se retiro del catalogo") String catalogItemName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2") int hintRevision,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El texto que el prompt le ensena al modelo sobre este articulo") String hintText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime publishedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "La cuenta de plataforma que firmo esta revision") Long publishedBySystemUserId,
        @Schema(description = "Cuando dejo de regir; nulo si es la vigente") LocalDateTime supersededAt,
        @Schema(description = "La cuenta de plataforma que la retiro. Nulo si la pista es la vigente"
                + " -no la ha retirado nadie- o si se reemplazo antes de que la columna existiera:"
                + " en ese caso no consta, y no es lo mismo que no haberse retirado") Long supersededBySystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean current,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static CatalogItemAiHintResponse from(CatalogItemAiHintDto dto) {
        return new CatalogItemAiHintResponse(dto.id(), dto.catalogItemId(), dto.catalogItemCode(),
                dto.catalogItemName(), dto.hintRevision(), dto.hintText(), dto.publishedAt(),
                dto.publishedBySystemUserId(), dto.supersededAt(), dto.supersededBySystemUserId(),
                dto.current(), dto.createdDate());
    }
}
