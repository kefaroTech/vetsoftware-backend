package com.vetsoftware.app.paymentrefund.infrastructure.web.request;

import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}, y no por el motivo de siempre.</strong> En un
 * recurso scoped al usuario la empresa se omite porque el cliente podria
 * suplantar a otra clinica; aqui la ruta es de plataforma y ese riesgo no
 * aplica, porque el puerto esta cerrado a {@code hasRole('SYSTEM')} a secas. La
 * razon es otra: la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira
 * <em>todo</em> {@code @RequestBody} sin mirar la ruta ni el rol. Tesoreria
 * sigue eligiendo a que clinica le devuelve — el {@code companyId} viaja como
 * {@code @RequestParam}, que es la forma que la regla si permite.
 *
 * @param destinationReference
 *            la cuenta o referencia a donde va el dinero. Obligatoria salvo
 *            cuando se devuelve al saldo a favor, donde no hay destino externo.
 *            La regla completa la valida el dominio, que es donde vive
 * @param clientRequestId
 *            llave de idempotencia. Con ella, el doble clic devuelve la
 *            devolucion que ya se registro en vez de sacar el dinero dos veces
 */
public record RegisterPaymentRefundRequest(
        @NotNull(message = "Debes indicar el pago que se devuelve.") Long paymentId,
        Long sourceDocumentId,
        @NotNull(message = "El valor de la devolucion es obligatorio.") @Positive(message = "El valor de la devolucion debe ser mayor que cero.") BigDecimal amount,
        @NotNull(message = "Debes indicar el medio por el que se devuelve.") RefundMethod method,
        @Size(max = 120, message = "La referencia de destino no puede superar los 120 caracteres.") String destinationReference,
        @NotNull(message = "Debes indicar cuando se devolvio el dinero.") LocalDateTime refundedAt,
        @NotNull(message = "Debes indicar la fecha valor de la devolucion.") LocalDate valueDate,
        @NotNull(message = "Debes indicar el codigo del motivo.") RefundReasonCode reasonCode,
        @NotBlank(message = "Debes explicar el motivo de la devolucion.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason,
        @NotNull(message = "Debes indicar quien autoriza la devolucion.") Long authorizedBySystemUserId,
        @Size(max = 64, message = "El identificador de la solicitud no puede superar los 64 caracteres.") String clientRequestId) {
}
