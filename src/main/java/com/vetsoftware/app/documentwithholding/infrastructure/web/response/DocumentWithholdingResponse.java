package com.vetsoftware.app.documentwithholding.infrastructure.web.response;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La retencion tal como sale por HTTP. La ven tanto la consola de plataforma
 * como el cliente: la retencion de un cliente es suya, y es el unico que la
 * puede imputar en su declaracion.
 *
 * <p>
 * <strong>{@code certificateId} va sin {@code REQUIRED} a proposito, y ese nulo
 * es el dato mas util de la respuesta</strong>: significa «practicada y sin
 * respaldo», que es exactamente lo que hay que reclamar antes de que el ano se
 * cierre. Marcarlo como obligatorio en el contrato haria que los dos fronts lo
 * tipasen como no nulo y perderian la distincion.
 *
 * <p>
 * <strong>{@code ratePercent} es porcentaje y viaja con sus seis
 * decimales.</strong> Quien pinte esta respuesta no debe multiplicar por cien:
 * {@code 0.690000} ya es 0,69 %, que es la tarifa de 6,9 por mil.
 */
public record DocumentWithholdingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long billingDocumentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) WithholdingType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal taxableBase,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal ratePercent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        String municipalityCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fiscalPeriodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate practicedOn,
        Long certificateId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static DocumentWithholdingResponse from(DocumentWithholdingDto dto) {
        return new DocumentWithholdingResponse(dto.id(), dto.companyId(), dto.billingDocumentId(),
                dto.type(), dto.taxableBase(), dto.ratePercent(), dto.amount(),
                dto.municipalityCode(), dto.fiscalYear(), dto.fiscalPeriodKey(), dto.practicedOn(),
                dto.certificateId(), dto.createdDate());
    }
}
