package com.vetsoftware.app.aiproposal.infrastructure.web.response;

import com.vetsoftware.app.aiproposal.application.dto.ProposalSuppressionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * El acuse de la supresion: tres contadores, su suma y las dos fechas.
 * <strong>No devuelve el correo</strong> -quien pregunta ya lo escribio- ni
 * ningun id de propuesta.
 *
 * <p>
 * <strong>{@code suppressedAt} es el reloj del SERVIDOR</strong>, y esa es toda
 * la razon de publicarlo: sin el, el front no tenia fecha que ensenarle al
 * titular y se fabricaba una con {@code Date.now()} del navegador. Inventar la
 * fecha central del acuse de una obligacion legal es exactamente el dato falso
 * que no se puede permitir aqui.
 *
 * <p>
 * &#9940; <strong>{@code previouslySuppressedAt} es nulable y por eso va
 * {@code NOT_REQUIRED}</strong>: es {@code null} en la primera peticion de un
 * titular. Es lo que impide leer un acuse de ceros como "aqui no habia nada"
 * cuando lo que dice es "ya se le borro el 3 de julio".
 */
public record ProposalSuppressionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int proposals,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int turns,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int lines,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int total,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime suppressedAt,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDateTime previouslySuppressedAt) {

    public static ProposalSuppressionResponse from(ProposalSuppressionDto dto) {
        return new ProposalSuppressionResponse(dto.proposals(), dto.turns(), dto.lines(),
                dto.total(), dto.suppressedAt(), dto.previouslySuppressedAt());
    }
}
