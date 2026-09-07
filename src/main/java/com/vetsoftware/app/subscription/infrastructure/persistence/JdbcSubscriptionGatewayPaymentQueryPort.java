package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.SubscriptionGatewayPaymentQueryPort;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lee directo las tres tablas del dinero de suscripciones, sin importar ninguna
 * clase de {@code subscriptionbilling} ni de {@code subscriptionpayment}: solo
 * lectura, y evita el N+1 de recorrer los documentos del contrato uno a uno.
 * Mismo criterio de SQL nativo aislado que {@code JdbcDianJobLeasePort}.
 */
@Component
public class JdbcSubscriptionGatewayPaymentQueryPort
        implements
            SubscriptionGatewayPaymentQueryPort {

    private static final String EXISTS_PENDING = """
            SELECT EXISTS (
                SELECT 1
                FROM subscription_billing_documents sbd
                JOIN billing_document_applications bda
                    ON bda.target_document_id = sbd.id AND bda.company_id = sbd.company_id
                JOIN subscription_payments sp
                    ON sp.id = bda.payment_id AND sp.company_id = bda.company_id
                WHERE sbd.company_id = ?
                  AND sbd.subscription_id = ?
                  AND bda.source_kind = 'PAYMENT'
                  AND sp.status = 'PENDING'
            )
            """;

    private static final String PERIOD_PAID = """
            SELECT EXISTS (
                SELECT 1
                FROM subscription_billing_documents sbd
                JOIN billing_document_applications bda
                    ON bda.target_document_id = sbd.id AND bda.company_id = sbd.company_id
                JOIN subscription_payments sp
                    ON sp.id = bda.payment_id AND sp.company_id = bda.company_id
                WHERE sbd.company_id = ?
                  AND sbd.subscription_id = ?
                  AND sbd.period_start = ?
                  AND sbd.period_end = ?
                  AND sbd.issue_status <> 'VOIDED'
                  AND bda.source_kind = 'PAYMENT'
                  AND sp.status = 'CONFIRMED'
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSubscriptionGatewayPaymentQueryPort(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsPendingPayment(Long companyId, Long subscriptionId) {
        Boolean exists = jdbcTemplate.queryForObject(EXISTS_PENDING, Boolean.class, companyId,
                subscriptionId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean isPeriodPaid(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd) {
        Boolean paid = jdbcTemplate.queryForObject(PERIOD_PAID, Boolean.class, companyId,
                subscriptionId, periodStart, periodEnd);
        return Boolean.TRUE.equals(paid);
    }
}
