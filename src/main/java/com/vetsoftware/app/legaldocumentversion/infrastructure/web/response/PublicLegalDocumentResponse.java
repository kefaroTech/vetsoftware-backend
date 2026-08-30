package com.vetsoftware.app.legaldocumentversion.infrastructure.web.response;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El aviso legal tal y como lo ve <strong>quien no tiene cuenta</strong>.
 *
 * <p>
 * &#9940; <strong>Existe por una sola diferencia con
 * {@link LegalDocumentVersionResponse}: aqui NO viaja
 * {@code publishedBySystemUserId}.</strong> Esa columna es la firma humana,
 * nominal y auditable de quien publico el texto —el mismo criterio que
 * {@code price_lists.published_by_system_user_id}, y por el que el changeset
 * 311 se niega a inventar una cuenta tecnica—, y la ruta
 * {@code GET /legal-documents/&#123;code&#125;/current} es publica y anonima:
 * la sirve cualquiera con {@code curl} y sin credenciales.
 *
 * <p>
 * <strong>Que se filtraba, exactamente.</strong> Un identificador interno y
 * consecutivo de la tabla de administradores de plataforma. No es una
 * credencial, y por eso la fuga es pequena; pero es enumerable —publicando dos
 * textos se ve el rango—, es la mitad de un ataque de fuerza bruta sobre la
 * consola de plataforma, y sobre todo <strong>no le sirve absolutamente de nada
 * a quien lee el aviso de privacidad</strong>. Un campo que no tiene consumidor
 * y si tiene lector no autorizado no se documenta: se quita.
 *
 * <p>
 * <strong>El resto de campos se conserva, incluido
 * {@code contentHash}</strong>, que es el que el front tiene que guardar con la
 * aceptacion: sin el, el cliente no podria despues volver a pedir el texto
 * exacto que acepto, que es la mitad de la prueba del articulo 9 de la Ley
 * 1581.
 *
 * <p>
 * <strong>Un record aparte y no un campo nulo.</strong> Anular el campo en la
 * respuesta de siempre dejaria el contrato anunciando un {@code Long}
 * obligatorio que llega vacio: el front lo tiparia como no nulo y romperia en
 * el navegador, que es exactamente el fallo que {@code api/openapi.json} existe
 * para impedir. Las otras tres operaciones del controller siguen exigiendo
 * identidad y siguen devolviendo {@link LegalDocumentVersionResponse} con la
 * firma dentro, que es donde si tiene sentido: quien administra la plataforma
 * necesita saber quien publico que.
 */
public record PublicLegalDocumentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "PRIVACY_NOTICE") String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "3") int documentVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LegalDocumentKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "SHA-256 del contenido: la huella con la que se prueba que texto se acepto") String contentHash,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime publishedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate effectiveFrom,
        LocalDateTime supersededAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean current,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static PublicLegalDocumentResponse from(LegalDocumentVersionDto dto) {
        return new PublicLegalDocumentResponse(dto.id(), dto.code(), dto.documentVersion(),
                dto.kind(), dto.title(), dto.content(), dto.contentHash(), dto.publishedAt(),
                dto.effectiveFrom(), dto.supersededAt(), dto.current(), dto.createdDate());
    }
}
