package com.vetsoftware.app.securityincident.infrastructure.web.response;

import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.securityincident.domain.AffectedScope;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una clinica alcanzada, tal como sale por HTTP.
 *
 * <p>
 * <strong>Este cuerpo no llega nunca a un tenant.</strong> Lo que una clinica
 * puede saber es que hubo un incidente que la alcanzo; cuantas mas hubo, cuales
 * y cuantos titulares de cada una es informacion de las demas. Por eso el unico
 * endpoint que lo devuelve esta cerrado a {@code ROLE_SYSTEM} a secas.
 *
 * <p>
 * <strong>Publica el {@code companyId} pelado y ningun dato de la
 * empresa.</strong> La rodaja valida que la clinica existe pero no lee un solo
 * campo suyo: quien pinta la pantalla ya tiene el censo de empresas, y traer
 * aqui su nombre ataria esta respuesta a la forma de otra feature sin ganar
 * nada.
 */
public record AffectedCompanyResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long securityIncidentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AffectedScope affectedScope,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Los titulares de esa clinica, no los del incidente entero.") int affectedSubjectCount) {

    public static AffectedCompanyResponse from(AffectedCompanyDto dto) {
        return new AffectedCompanyResponse(dto.id(), dto.securityIncidentId(), dto.companyId(),
                dto.affectedScope(), dto.affectedSubjectCount());
    }
}
