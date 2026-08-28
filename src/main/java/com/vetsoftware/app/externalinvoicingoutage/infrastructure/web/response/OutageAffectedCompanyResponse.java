package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.response;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una clinica del reparto de una caida, tal como sale por HTTP.
 *
 * <p>
 * <strong>Esta forma NO la ve ningun cliente.</strong> El unico endpoint que la
 * devuelve esta cerrado a {@code hasRole('SYSTEM')} a secas, y no por descuido:
 * un tenant puede saber que hubo una caida, <b>nunca a cuantos alcanzo ni a
 * quienes</b>. Acotar el listado por {@code outageId} no lo convierte en propio
 * —la caida es de todos—, que es el mismo criterio con el que BE-29 descarta
 * {@code findAllByAnimalId}.
 *
 * <p>
 * <strong>Sale {@code companyId} pelado y no un resumen de la empresa</strong>:
 * la puente no guarda ni el nombre ni el NIT, y fabricarlos aqui obligaria a
 * esta rodaja a consultar otra feature para adornar una respuesta que solo lee
 * plataforma.
 *
 * <p>
 * Sin {@code version} ni {@code createdDate}: la tabla puente no tiene ninguna
 * de las dos columnas.
 */
public record OutageAffectedCompanyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long outageId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El numero que sostiene la reclamacion de esa clinica.") int failedDocumentCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OutageResolution resolvedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Si salio con numeracion de contingencia, que es lo que hay que demostrar.") boolean contingencyNumbering) {

    public static OutageAffectedCompanyResponse from(OutageAffectedCompanyDto dto) {
        return new OutageAffectedCompanyResponse(dto.id(), dto.outageId(), dto.companyId(),
                dto.failedDocumentCount(), dto.resolvedBy(), dto.contingencyNumbering());
    }
}
