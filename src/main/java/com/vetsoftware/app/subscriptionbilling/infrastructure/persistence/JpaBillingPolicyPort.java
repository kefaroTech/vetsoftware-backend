package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingPolicyPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lee de {@code platform_billing_config} lo único que la capa de cobro
 * necesita: los días de plazo por defecto.
 *
 * <p>
 * <b>Falla si no hay configuración, y falla legible.</b> La alternativa —un
 * valor por defecto en el código— pondría vencimientos que nadie decidió sobre
 * facturas reales, y el error solo se vería cuando una clínica al día
 * apareciera en mora. Es el mismo criterio con el que la especificación pide
 * que el arranque en vacío falle en vez de degradarse.
 *
 * <p>
 * Consulta nativa contra la tabla por el mismo motivo que en
 * {@link JpaSubscriptionQueryPort}: hace falta un escalar, no la entidad de
 * otro slice.
 */
@Component
public class JpaBillingPolicyPort implements BillingPolicyPort {

    private static final String SELECT_PLAZO = """
            SELECT c.default_payment_term_days
            FROM platform_billing_config c
            WHERE c.singleton = 1
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int defaultPaymentTermDays() {
        List<?> filas = entityManager.createNativeQuery(SELECT_PLAZO).setMaxResults(1)
                .getResultList();
        if (filas.isEmpty() || filas.get(0) == null)
            throw new IllegalStateException(
                    "platform_billing_config has no row: there is no payment term to count the"
                            + " due date from, and guessing one would put made-up due dates on"
                            + " real invoices");
        return ((Number) filas.get(0)).intValue();
    }
}
