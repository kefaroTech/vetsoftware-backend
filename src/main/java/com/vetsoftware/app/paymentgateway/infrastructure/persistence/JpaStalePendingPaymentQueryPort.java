package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.StalePendingPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.domain.StalePendingPayment;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Consume {@code SubscriptionPaymentRepository.findStalePendingGatewayPayments}
 * de {@code subscriptionpayment}: ya llega ascendente por {@code receivedAt} y
 * acotada a {@code limit} filas, así que este adaptador solo traduce el dominio
 * ajeno a {@link StalePendingPayment}.
 */
@Component
public class JpaStalePendingPaymentQueryPort implements StalePendingPaymentQueryPort {

    private final SubscriptionPaymentRepository repository;

    public JpaStalePendingPaymentQueryPort(SubscriptionPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StalePendingPayment> findOlderThan(LocalDateTime cutoff, int limit) {
        return repository.findStalePendingGatewayPayments(cutoff, limit).stream()
                .map(payment -> new StalePendingPayment(payment.getCompanyId(), payment.getId(),
                        payment.getGatewayReference(), payment.getAmount(),
                        payment.getClientRequestId(), payment.getReceivedAt()))
                .toList();
    }
}
