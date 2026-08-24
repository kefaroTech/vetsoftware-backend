package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import org.springframework.stereotype.Component;

@Component("subscriptionPaymentJpaBillingDocumentSettlementPort")
public class JpaBillingDocumentSettlementPort implements BillingDocumentSettlementPort {

    private final BillingDocumentSettlementJpaRepository settlementJpaRepository;

    public JpaBillingDocumentSettlementPort(
            BillingDocumentSettlementJpaRepository settlementJpaRepository) {
        this.settlementJpaRepository = settlementJpaRepository;
    }

    @Override
    public int recalculateSettledAmount(Long documentId, Long companyId) {
        return settlementJpaRepository.recalculateSettledAmount(documentId, companyId);
    }
}
