package com.vetsoftware.app.accountingaccount.infrastructure.web.request;

import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId} por ninguna via</strong>, ni siquiera como
 * {@code @RequestParam}: el plan de cuentas no tiene empresa a la que apuntar.
 *
 * @param parentCode
 *            codigo de la cuenta padre. Obligatorio si y solo si el nivel es
 *            mayor que 1; el «si y solo si» lo valida el dominio, que es donde
 *            vive porque mira dos campos
 * @param accountLevel
 *            1 clase, 2 grupo, 4 cuenta, 6 subcuenta. La lista cerrada la
 *            comprueba el dominio, no un {@code @Min}/{@code @Max}: no es un
 *            rango, son cuatro valores
 * @param postable
 *            solo el nivel 6 admite asiento
 */
public record CreateAccountingAccountRequest(
        @NotBlank(message = "Debes indicar el codigo de la cuenta.") @Size(max = 10, message = "El codigo no puede superar los 10 caracteres.") String code,
        @NotBlank(message = "Debes indicar el nombre de la cuenta.") @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.") String name,
        @NotNull(message = "Debes indicar la clase de la cuenta.") AccountClass accountClass,
        @Size(max = 10, message = "El codigo de la cuenta padre no puede superar los 10 caracteres.") String parentCode,
        @NotNull(message = "Debes indicar el nivel de la cuenta.") @Schema(description = "1 clase, 2 grupo, 4 cuenta, 6 subcuenta.") Integer accountLevel,
        @NotNull(message = "Debes indicar si la cuenta admite asiento.") @Schema(description = "Solo el nivel 6 puede admitir asiento.") Boolean postable,
        @NotNull(message = "Debes indicar si la cuenta exige tercero identificado.") Boolean requiresThirdParty,
        @NotNull(message = "Debes indicar desde cuando aplica la cuenta.") LocalDate validFrom,
        @Schema(description = "Nulo abre la vigencia; con fecha la cuenta entra ya cerrada.") LocalDate validTo) {
}
