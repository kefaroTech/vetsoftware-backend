package com.vetsoftware.app.subscription.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Alta de linea. {@code clientRequestId} es obligatorio: es la llave que impide
 * que dos clics en «Anadir» generen dos lineas y dos cobros.
 *
 * <p>
 * <strong>Quien firma el otrosi no viaja en el cuerpo</strong>, igual que no
 * viaja {@code companyId} y por el mismo motivo. Lo inyecta el controller desde
 * el principal —{@code authz.currentEmployeeIdOrNull()} o
 * {@code authz.currentSystemUserIdOrNull()}—: un rastro de auditoria que
 * escribe el propio auditado no es un rastro de auditoria, y con la firma en el
 * cuerpo cualquiera con acceso al endpoint podria atribuirle a otro haber
 * pedido la enmienda.
 *
 *
 * <p>
 * Tampoco trae numero de otrosi: {@code amendment_number} es un numero citable
 * y lo reserva el servidor de forma serializada, dentro de la misma
 * transaccion.
 *
 * <p>
 * <strong>Y tampoco trae los importes.</strong> {@code prorationAmount} y
 * {@code monthlyDeltaAmount} viajaban aqui y se persistian tal cual, es decir:
 * el importe lo dictaba quien mandaba la peticion. Hoy los calcula el servidor
 * con {@code ProrationCalculator}, sobre el periodo de facturacion en curso del
 * contrato y la fecha efectiva. Mandarlos ya no es posible.
 */
public record AddSubscriptionItemRequest(@NotBlank @Size(max = 64) String clientRequestId,
        @NotNull LocalDate effectiveDate, @Size(max = 255) String reason, Long quoteId,
        @NotNull @Valid SubscriptionItemLineRequest line) {
}
