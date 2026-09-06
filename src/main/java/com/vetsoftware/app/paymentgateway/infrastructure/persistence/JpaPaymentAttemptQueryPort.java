package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaEntity;
import com.vetsoftware.app.paymentattempt.infrastructure.persistence.PaymentAttemptJpaRepository;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.LastPaymentAttempt;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Consume {@code PaymentAttemptJpaRepository} de {@code paymentattempt} por sus
 * variantes ya acotadas por empresa, sin importar su dominio salvo por el
 * {@code DeclineKind} que exige la firma de {@code countChargeableSince}.
 */
@Component
public class JpaPaymentAttemptQueryPort implements PaymentAttemptQueryPort {

    private final PaymentAttemptJpaRepository repository;

    public JpaPaymentAttemptQueryPort(PaymentAttemptJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LastPaymentAttempt> findLast(Long companyId, Long billingDocumentId) {
        Pageable lastOne = Pages.request(0, 1, Sort.by(Sort.Direction.DESC, "attemptNumber"));
        return repository
                .findAllByCompanyIdAndBillingDocumentId(companyId, billingDocumentId, lastOne)
                .stream().findFirst().map(this::toLastAttempt);
    }

    @Override
    public int countRetryableSince(Long companyId, Long billingDocumentId, LocalDateTime since) {
        return Math.toIntExact(repository.countChargeableSince(companyId, billingDocumentId, since,
                DeclineKind.CONFIGURATION));
    }

    private LastPaymentAttempt toLastAttempt(PaymentAttemptJpaEntity entity) {
        return new LastPaymentAttempt(entity.getAttemptNumber(),
                GatewayDeclineKind.valueOf(entity.getDeclineKind().name()), entity.getAttemptedAt(),
                entity.getNextAttemptAt());
    }
}
