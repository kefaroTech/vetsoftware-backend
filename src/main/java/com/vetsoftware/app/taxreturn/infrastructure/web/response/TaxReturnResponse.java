package com.vetsoftware.app.taxreturn.infrastructure.web.response;

import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La declaracion tal como sale por HTTP. Solo la ve la consola de plataforma:
 * cero superficie de cliente.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> ni ninguna de las tres columnas
 * generadas ({@code municipality_key}, {@code vat_frequency_year},
 * {@code current_return_marker}): son detalle del motor y publicarlas invitaria
 * a construir logica sobre un centinela de base de datos.
 */
public record TaxReturnResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaxKind taxKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fiscalPeriodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "1 la inicial; sube con cada correccion.") int sequenceNumber,
        @Schema(description = "Codigo DIVIPOLA. Presente solo en las declaraciones de ICA.") String municipalityCode,
        @Schema(description = "Presente solo en las declaraciones de IVA.") VatFrequency vatFrequency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaxReturnStatus status,
        LocalDateTime filedAt, Long filedBySystemUserId,
        @Schema(description = "El radicado. Presente solo si ya se presento.") String receiptRef,
        String fileRef,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalGenerated,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalDeductible,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal balancePayable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal balanceCredit,
        @Schema(description = "Hasta cuando pueden revisarla. Existe siempre que este presentada.") LocalDate firmezaUntil,
        @Schema(description = "La declaracion a la que corrige. Vacio en la inicial.") Long correctsReturnId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static TaxReturnResponse from(TaxReturnDto dto) {
        return new TaxReturnResponse(dto.id(), dto.taxKind(), dto.fiscalYear(),
                dto.fiscalPeriodKey(), dto.sequenceNumber(), dto.municipalityCode(),
                dto.vatFrequency(), dto.status(), dto.filedAt(), dto.filedBySystemUserId(),
                dto.receiptRef(), dto.fileRef(), dto.totalGenerated(), dto.totalDeductible(),
                dto.balancePayable(), dto.balanceCredit(), dto.firmezaUntil(),
                dto.correctsReturnId(), dto.createdDate());
    }
}
