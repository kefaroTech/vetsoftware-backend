package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.ReconcileSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReconcileSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuadra el pago contra el extracto bancario. Solo un pago {@code CONFIRMED} se
 * puede conciliar: marcar como cuadrado uno que la pasarela nunca confirmo es
 * como aparece plata en la cartera sin haber entrado en el banco.
 */
@Observed(name = "subscription.payment.reconcile")
@Service
public class ReconcileSubscriptionPaymentService implements ReconcileSubscriptionPaymentUseCase {

    private final SubscriptionPaymentRepository repository;
    private final Clock clock;

    public ReconcileSubscriptionPaymentService(SubscriptionPaymentRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionPaymentDto execute(ReconcileSubscriptionPaymentCommand command) {
        SubscriptionPayment payment = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionPaymentNotFoundException(command.id()));
        payment.reconcile(LocalDateTime.now(clock));
        return SubscriptionPaymentDto.from(repository.save(payment));
    }
}
