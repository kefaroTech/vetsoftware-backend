package com.vetsoftware.app.accountingaccount.infrastructure.web.response;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La cuenta contable tal como sale por HTTP. Solo la ve la consola de
 * plataforma: son los libros de Lumbre.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato de la cuenta—.
 */
public record AccountingAccountResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AccountClass accountClass,
        @Schema(description = "Vacio solo en la raiz del plan.") String parentCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "1 clase, 2 grupo, 4 cuenta, 6 subcuenta.") int accountLevel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Solo el nivel 6 admite asiento.") boolean postable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean requiresThirdParty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom,
        @Schema(description = "Nulo mientras la vigencia siga abierta.") LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static AccountingAccountResponse from(AccountingAccountDto dto) {
        return new AccountingAccountResponse(dto.id(), dto.code(), dto.name(), dto.accountClass(),
                dto.parentCode(), dto.accountLevel(), dto.postable(), dto.requiresThirdParty(),
                dto.validFrom(), dto.validTo(), dto.createdDate(), dto.enabled());
    }
}
