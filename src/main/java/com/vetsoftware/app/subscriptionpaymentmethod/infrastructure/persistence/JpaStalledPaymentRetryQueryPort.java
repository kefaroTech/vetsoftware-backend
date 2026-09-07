package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaEntity;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.StalledPaymentRetryQueryPort;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class JpaStalledPaymentRetryQueryPort implements StalledPaymentRetryQueryPort {

    private final PaymentAttemptJpaRepository paymentAttemptJpaRepository;

    public JpaStalledPaymentRetryQueryPort(
            PaymentAttemptJpaRepository paymentAttemptJpaRepository) {
        this.paymentAttemptJpaRepository = paymentAttemptJpaRepository;
    }

    @Override
    public List<Long> findStalledLastAttemptIds(Long companyId) {
        return Stream.of(DeclineKind.CONFIGURATION, DeclineKind.HARD)
                .flatMap(kind -> paymentAttemptJpaRepository
                        .findLastAttemptsByCompanyIdAndDeclineKind(companyId, kind).stream())
                .map(PaymentAttemptJpaEntity::getId).toList();
    }
}
