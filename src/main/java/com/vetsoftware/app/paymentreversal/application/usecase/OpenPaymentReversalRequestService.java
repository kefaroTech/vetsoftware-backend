package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.command.OpenPaymentReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.OpenPaymentReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.application.port.out.SubscriptionPaymentQueryPort;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.paymentreversal.domain.ReversalRequestAlreadyExistsException;
import com.vetsoftware.app.paymentreversal.domain.SubscriptionPaymentRef;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre el expediente de una reversion.
 *
 * <p>
 * Dos comprobaciones antes de escribir, y las dos existen para que el fallo
 * llegue con un mensaje util en vez de como un error de integridad:
 * <ol>
 * <li>el pago existe <strong>y es de esta empresa</strong> —el puerto solo
 * ofrece la variante acotada, asi que no hay forma de colgar el expediente del
 * cobro de otro tenant—;
 * <li>no hay ya un expediente sobre ese pago
 * ({@code uq_payment_reversal_requests_payment}).
 * </ol>
 */
@Observed(name = "payment.reversal.open")
@Service
public class OpenPaymentReversalRequestService implements OpenPaymentReversalRequestUseCase {

    private final PaymentReversalRequestRepository repository;
    private final SubscriptionPaymentQueryPort paymentQueryPort;
    private final Clock clock;

    public OpenPaymentReversalRequestService(PaymentReversalRequestRepository repository,
            SubscriptionPaymentQueryPort paymentQueryPort, Clock clock) {
        this.repository = repository;
        this.paymentQueryPort = paymentQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentReversalRequestDto execute(OpenPaymentReversalRequestCommand command) {
        SubscriptionPaymentRef payment = paymentQueryPort
                .findByIdAndCompanyId(command.paymentId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription payment not found: " + command.paymentId()));

        repository.findByCompanyIdAndPaymentId(command.companyId(), payment.id())
                .ifPresent(existing -> {
                    throw new ReversalRequestAlreadyExistsException(payment.id());
                });

        PaymentReversalRequest opened = PaymentReversalRequest.open(command.companyId(),
                payment.id(), command.origin(), command.causal(), command.consumerDetermination(),
                command.consumerBecameAwareAt(), command.claimReceivedAt(),
                command.issuerNotifiedAt(), command.claimEvidenceRef(), command.deadlineAt(),
                LocalDateTime.now(clock));
        return PaymentReversalRequestDto.from(repository.save(opened));
    }
}
