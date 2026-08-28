package com.vetsoftware.app.bankreceipt.infrastructure.web.response;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La entrada del extracto tal como sale por HTTP. <strong>Solo la ve la consola
 * de plataforma</strong>: no hay camino de tenant en esta feature.
 *
 * <p>
 * {@code identifiedAt} va sin {@code REQUIRED} a proposito: es nulo mientras la
 * entrada sigue en la bandeja, y marcarlo obligatorio haria que el tipo
 * generado para el front prometiera un valor que la mitad de las filas no
 * tiene.
 */
public record BankReceiptResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bankAccountRef,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bankReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate receivedOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount, String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BankReceiptStatus status,
        LocalDateTime identifiedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static BankReceiptResponse from(BankReceiptDto dto) {
        return new BankReceiptResponse(dto.id(), dto.bankAccountRef(), dto.bankReference(),
                dto.receivedOn(), dto.amount(), dto.description(), dto.status(), dto.identifiedAt(),
                dto.createdDate());
    }
}
