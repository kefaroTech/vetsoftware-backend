package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.AssignGatewayReferenceCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.AssignGatewayReferenceUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "subscription.payment.assign.gateway.reference")
@Service
public class AssignGatewayReferenceService implements AssignGatewayReferenceUseCase {

    private final SubscriptionPaymentRepository repository;

    public AssignGatewayReferenceService(SubscriptionPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SubscriptionPaymentDto execute(AssignGatewayReferenceCommand command) {
        SubscriptionPayment payment = repository
                .lockByIdAndCompanyId(command.paymentId(), command.companyId())
                .orElseThrow(() -> new SubscriptionPaymentNotFoundException(command.paymentId()));
        payment.assignGatewayReference(command.gatewayReference(), command.gatewayCreatedAt());
        return SubscriptionPaymentDto.from(repository.save(payment));
    }
}
