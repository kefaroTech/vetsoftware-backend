package com.vetsoftware.app.companytrialgrant.infrastructure.web.response;

import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una concesión de prueba tal como la ven los frontends.
 *
 * <p>
 * <strong>Lleva {@code effectiveDays} además de {@code daysGranted}</strong>, y
 * eso no es redundancia: son números distintos en cuanto la ventana recorta —un
 * módulo añadido el día 15 de una ventana de 30 recibe 15, no 30— y enseñar el
 * segundo cuando el real es el primero es prometerle al cliente días que no va
 * a tener.
 *
 * <p>
 * {@code live} viaja calculado por la misma razón que {@code open} en la
 * ventana: «viva» depende de la ausencia de {@code consumedAt} y de la fecha, y
 * dejar esa conjunción a cada pantalla es garantizar que dos pantallas
 * discrepen.
 */
public record CompanyTrialGrantResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long trialWindowId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate grantedOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int daysGranted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Días reales tras el recorte de la ventana") int effectiveDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate trialEndDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int policyTrialDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TrialPolicyOutcome policyTrialOutcome,
        Long sourceQuoteId, Long grantingAmendmentId, LocalDateTime consumedAt,
        TrialOutcome outcome, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean live) {

    public static CompanyTrialGrantResponse from(CompanyTrialGrantDto dto) {
        return new CompanyTrialGrantResponse(dto.id(), dto.companyId(), dto.catalogItemId(),
                dto.trialWindowId(), dto.grantedOn(), dto.daysGranted(), dto.effectiveDays(),
                dto.trialEndDate(), dto.policyTrialDays(), dto.policyTrialOutcome(),
                dto.sourceQuoteId(), dto.grantingAmendmentId(), dto.consumedAt(), dto.outcome(),
                dto.live());
    }
}
