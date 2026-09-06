package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentattempt.application.command.RecordPaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.port.in.RecordPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code paymentattempt} la anotación de un cobro rechazado.
 *
 * <p>
 * <strong>Recorta el código aquí, no antes.</strong> El límite es una
 * restricción de la tabla de otra feature
 * ({@code payment_attempts.gateway_decline_code}), y este adaptador es el único
 * punto de este slice que lo conoce: si cambia, cambia en un solo sitio.
 *
 * <p>
 * El tope se repite aquí en vez de leerse de
 * {@code PaymentAttempt.MAX_DECLINE_CODE_LENGTH} porque el vertical slicing
 * prohíbe importar el dominio de otra rodaja; los dos valores son el espejo de
 * {@code payment_attempts.gateway_decline_code} (changeset 409).
 */
@Component
public class PaymentAttemptRecorderAdapter implements PaymentAttemptRecorderPort {

    private static final int MAX_DECLINE_CODE_LENGTH = 160;

    private final RecordPaymentAttemptUseCase recordUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PaymentAttemptRecorderAdapter(RecordPaymentAttemptUseCase recordUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.recordUseCase = recordUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void record(Long companyId, Long billingDocumentId, Long paymentMethodId, String gateway,
            BigDecimal requestedAmount, String gatewayDeclineCode, GatewayDeclineKind declineKind,
            LocalDateTime attemptedAt, LocalDateTime nextAttemptAt) {
        String code = truncate(gatewayDeclineCode);
        systemAuthRunner.run(() -> recordUseCase.execute(new RecordPaymentAttemptCommand(companyId,
                billingDocumentId, paymentMethodId, gateway, requestedAmount, code,
                DeclineKind.valueOf(declineKind.name()), attemptedAt, nextAttemptAt)));
    }

    private static String truncate(String code) {
        if (code == null || code.length() <= MAX_DECLINE_CODE_LENGTH)
            return code;
        return code.substring(0, MAX_DECLINE_CODE_LENGTH);
    }
}
