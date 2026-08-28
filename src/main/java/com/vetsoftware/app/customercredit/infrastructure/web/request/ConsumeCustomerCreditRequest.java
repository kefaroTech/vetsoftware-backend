package com.vetsoftware.app.customercredit.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Aplicacion de saldo a favor a un documento de cobro. <strong>Sin la empresa
 * en el cuerpo</strong> ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}): viaja como
 * {@code @RequestParam}, por el mismo motivo que en
 * {@link GrantCustomerCreditRequest}.
 *
 * @param clientRequestId
 *            llave de idempotencia de la operacion. <strong>Acotada a 56
 *            caracteres y sin el separador</strong>: un consumo escribe una
 *            fila por lote y cada una necesita su propia llave, asi que el
 *            servicio le anade un sufijo. Los 56 dejan sitio al sufijo dentro
 *            de los 64 de la columna, y prohibir el separador impide que la
 *            llave de una operacion se confunda con el prefijo de otra
 */
public record ConsumeCustomerCreditRequest(
        @NotNull(message = "El valor a aplicar es obligatorio.") @Positive(message = "El valor a aplicar debe ser mayor que cero.") BigDecimal amount,
        @NotNull(message = "Debes indicar el documento al que se aplica el saldo.") Long originDocumentId,
        @NotBlank(message = "El identificador de la solicitud es obligatorio.") @Size(max = 56, message = "El identificador de la solicitud no puede superar los 56 caracteres.") @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "El identificador de la solicitud solo admite letras, digitos y los signos _ . : y -.") String clientRequestId) {
}
