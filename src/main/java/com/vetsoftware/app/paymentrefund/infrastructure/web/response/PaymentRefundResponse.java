package com.vetsoftware.app.paymentrefund.infrastructure.web.response;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que ve <strong>el cliente</strong> de una devolucion. El documento maestro
 * reparte el bloque «Cobro y saldos» como <em>escribe plataforma, leen
 * ambos</em>, y una devolucion es plata del cliente.
 *
 * <p>
 * <strong>No declara {@code authorizedBySystemUserId}, y esa ausencia es la
 * regla, no un olvido.</strong> Ese campo es el id interno del operador de
 * VetSoftware que autorizo la salida de caja: entero pequeño y enumerable, y
 * visible para cualquier empleado con permiso de lectura de cualquier clinica
 * permite mapear la plantilla interna y correlacionar que operador atiende a
 * que clinica. Quien necesita el dato es la tesoreria, y lo publica
 * {@link SystemPaymentRefundResponse}.
 *
 * <p>
 * <strong>El recorte vive aqui y no en el DTO a proposito.</strong> El
 * {@code record} es la frontera: {@code PaymentRefundDto} transporta el id
 * porque plataforma lo necesita. Ponerlo mas adentro obligaria a dos DTO casi
 * iguales y dejaria a plataforma sin el dato; ponerlo mas afuera —confiando en
 * que el controller no lo copie, o en un {@code if} que alguien puede olvidar—
 * seria una promesa en vez de un tipo. Anadir el campo a este record es una
 * fuga, y se ve en el diff.
 *
 * <p>
 * Tampoco lleva {@code clientRequestId}: la llave de idempotencia es una
 * barandilla del que escribe, no un dato del expediente.
 */
public record PaymentRefundResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long paymentId, Long sourceDocumentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundMethod method,
        String destinationReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime refundedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate valueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundReasonCode reasonCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static PaymentRefundResponse from(PaymentRefundDto dto) {
        return new PaymentRefundResponse(dto.id(), dto.companyId(), dto.paymentId(),
                dto.sourceDocumentId(), dto.amount(), dto.method(), dto.destinationReference(),
                dto.refundedAt(), dto.valueDate(), dto.reasonCode(), dto.reason(),
                dto.createdDate());
    }
}
