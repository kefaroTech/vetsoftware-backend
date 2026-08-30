package com.vetsoftware.app.aiproposal.infrastructure.web.response;

import com.vetsoftware.app.aiproposal.application.dto.ProposalSuppressionDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El acuse de la supresion: tres contadores y su suma. <strong>No devuelve el
 * correo</strong> -quien pregunta ya lo escribio- ni ningun id de propuesta.
 */
public record ProposalSuppressionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int proposals,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int turns,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int lines,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int total) {

    public static ProposalSuppressionResponse from(ProposalSuppressionDto dto) {
        return new ProposalSuppressionResponse(dto.proposals(), dto.turns(), dto.lines(),
                dto.total());
    }
}
