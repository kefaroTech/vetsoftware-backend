package com.vetsoftware.app.platformtaxprofile.infrastructure.web.response;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La identidad fiscal de Lumbre tal como sale por HTTP.
 *
 * <p>
 * <strong>Solo la ve la consola de plataforma.</strong> Los cinco endpoints van
 * bajo {@code /system} y sus puertos estan cerrados a {@code hasRole('SYSTEM')}
 * a secas.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato de la identidad— ni la columna generada
 * {@code current_profile_marker}: es detalle del motor, existe para que un
 * indice unico pueda restringir lo que con {@code NULL} no restringia, y
 * publicarla invitaria a construir logica sobre un centinela de base de datos.
 * Lo que el front necesita para saber cual rige es {@code validTo == null}.
 *
 * @param validTo
 *            nulo significa <em>vigente</em>
 * @param economicActivity
 *            nulo es legitimo: {@code economic_activity_id} es nulable
 */
public record PlatformTaxProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PlatformDocumentType documentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String documentId,
        @Schema(description = "Solo el NIT lo lleva.") String verificationDigit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Se imprime en la factura de cada cliente.") String legalName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PlatformTaxRegime taxRegime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fiscalEmail,
        String commercialName, PlatformEconomicActivitySummary economicActivity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Si Lumbre es autorretenedor. Hoy ningun calculo lo consume todavia.") boolean selfWithholder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom,
        @Schema(description = "Nulo mientras la vigencia siga abierta.") LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static PlatformTaxProfileResponse from(PlatformTaxProfileDto dto) {
        return new PlatformTaxProfileResponse(dto.id(), dto.documentType(), dto.documentId(),
                dto.verificationDigit(), dto.legalName(), dto.taxRegime(), dto.fiscalEmail(),
                dto.commercialName(), PlatformEconomicActivitySummary.from(dto.economicActivity()),
                dto.selfWithholder(), dto.validFrom(), dto.validTo(), dto.createdDate());
    }
}
