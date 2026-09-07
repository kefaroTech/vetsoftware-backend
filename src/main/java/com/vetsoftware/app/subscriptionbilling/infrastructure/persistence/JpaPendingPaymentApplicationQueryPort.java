package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.PendingPaymentApplicationQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaEntity;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class JpaPendingPaymentApplicationQueryPort implements PendingPaymentApplicationQueryPort {

    private final BillingDocumentApplicationJpaRepository repository;

    public JpaPendingPaymentApplicationQueryPort(
            BillingDocumentApplicationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsPendingApplication(Long companyId, Long billingDocumentId) {
        return applications(companyId, billingDocumentId).stream().anyMatch(
                application -> application.getSourceKind() == ApplicationSourceKind.PAYMENT
                        && application.getPayment() != null && application.getPayment()
                                .getStatus() == SubscriptionPaymentStatus.PENDING);
    }

    @Override
    public List<Long> findConfirmedApplicationIds(Long companyId, Long billingDocumentId) {
        return applications(companyId, billingDocumentId).stream()
                .filter(application -> application.getSourceKind() == ApplicationSourceKind.PAYMENT
                        && application.getPayment() != null
                        && application.getPayment()
                                .getStatus() == SubscriptionPaymentStatus.CONFIRMED)
                .map(BillingDocumentApplicationJpaEntity::getId).toList();
    }

    private List<BillingDocumentApplicationJpaEntity> applications(Long companyId,
            Long billingDocumentId) {
        return repository.findAllByTargetDocument_IdAndCompanyId(billingDocumentId, companyId,
                Pageable.unpaged()).getContent();
    }
}
