package com.vetsoftware.app.customercredit.infrastructure.web.response;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El saldo a favor de una empresa.
 *
 * <p>
 * {@code recalculatedAt} viaja a proposito: es la marca del ultimo cuadre
 * contra el libro, y sin ella el cliente no puede saber si lo que esta leyendo
 * es una proyeccion fresca o una que lleva meses sin recalcularse.
 */
public record CustomerCreditBalanceResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal balanceAmount,
        LocalDate nextExpiryOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime recalculatedAt,
        Long version) {

    public static CustomerCreditBalanceResponse from(CustomerCreditBalanceDto dto) {
        return new CustomerCreditBalanceResponse(dto.id(), dto.companyId(), dto.balanceAmount(),
                dto.nextExpiryOn(), dto.recalculatedAt(), dto.version());
    }
}
