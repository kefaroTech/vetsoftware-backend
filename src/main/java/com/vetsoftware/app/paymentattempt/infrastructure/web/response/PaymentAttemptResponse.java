package com.vetsoftware.app.paymentattempt.infrastructure.web.response;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que ve <strong>el cliente</strong> de un cobro que reboto.
 *
 * <p>
 * <strong>No declara {@code gatewayDeclineCode}, y esa ausencia es la regla, no
 * un olvido.</strong> El bloque <em>Cobro y saldos</em> del documento maestro
 * es explicito: el cliente ve lo suyo, <em>nunca el codigo de rechazo crudo de
 * la pasarela — solo su clase</em>. Aqui sale {@link DeclineKind}, que es lo
 * que le permite entender si tiene que cambiar de tarjeta ({@code HARD}),
 * esperar al reintento ({@code SOFT}) o no hacer nada porque el problema es
 * nuestro ({@code CONFIGURATION}).
 *
 * <p>
 * <strong>El recorte vive aqui y no en el DTO a proposito.</strong> El
 * {@code record} es la frontera: {@code PaymentAttemptDto} transporta el codigo
 * crudo porque plataforma lo necesita para revisar la traduccion, y
 * {@link SystemPaymentAttemptResponse} si lo publica. Poner el filtro mas
 * adentro obligaria a dos DTO casi iguales y dejaria a plataforma sin el dato;
 * ponerlo mas afuera —confiando en que el controller no lo copie— seria una
 * promesa en vez de un tipo. Anadir el campo a este record es una fuga, y se ve
 * en el diff.
 */
public record PaymentAttemptResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long billingDocumentId,
        Long paymentMethodId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer attemptNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String gateway,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal requestedAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Clase del rechazo. El codigo crudo de la pasarela no se expone.") DeclineKind declineKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime attemptedAt,
        LocalDateTime nextAttemptAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        Long version) {

    public static PaymentAttemptResponse from(PaymentAttemptDto dto) {
        return new PaymentAttemptResponse(dto.id(), dto.companyId(), dto.billingDocumentId(),
                dto.paymentMethodId(), dto.attemptNumber(), dto.gateway(), dto.requestedAmount(),
                dto.declineKind(), dto.attemptedAt(), dto.nextAttemptAt(), dto.createdDate(),
                dto.version());
    }
}
