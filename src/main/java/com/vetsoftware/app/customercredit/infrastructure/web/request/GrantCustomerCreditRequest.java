package com.vetsoftware.app.customercredit.infrastructure.web.request;

import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}: la excepcion para {@code /system} no
 * existe.</strong> El actor SYSTEM no tiene empresa propia y tiene que decir a
 * quien le abona, pero la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira
 * <em>todo</em> {@code @RequestBody} sin mirar la ruta ni el rol. La empresa
 * viaja como {@code @RequestParam}, igual que en
 * {@code SystemSubscriptionPaymentController}.
 *
 * @param clientRequestId
 *            llave de idempotencia, obligatoria: un abono sin ella es un doble
 *            clic esperando
 */
public record GrantCustomerCreditRequest(
        @NotNull(message = "El valor del abono es obligatorio.") @Positive(message = "El valor del abono debe ser mayor que cero.") BigDecimal amount,
        @NotNull(message = "Debes indicar el origen del saldo.") CreditOriginKind originKind,
        Long originPaymentId, Long originDocumentId, Long originSubscriptionId, LocalDate expiresOn,
        @NotBlank(message = "El identificador de la solicitud es obligatorio.") @Size(max = 64, message = "El identificador de la solicitud no puede superar los 64 caracteres.") String clientRequestId) {
}
