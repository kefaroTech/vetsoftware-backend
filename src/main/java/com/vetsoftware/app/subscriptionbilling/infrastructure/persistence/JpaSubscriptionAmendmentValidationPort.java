package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionAmendmentValidationPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Comprueba el otrosi del slice {@code subscription} <b>siempre acotado por
 * empresa</b>, con el mismo molde que {@link JpaSubscriptionQueryPort} y
 * {@link JpaSubscriptionItemValidationPort}.
 *
 * <p>
 * La consulta replica {@code fk_subscription_charges_amendment} sobre
 * {@code subscription_amendments(company_id, id)}. Esa tabla no tiene
 * {@code enabled} —un otrosi no se retira, se emite otro—, asi que el par de
 * columnas es toda la condicion.
 */
@Component
public class JpaSubscriptionAmendmentValidationPort implements SubscriptionAmendmentValidationPort {

    private static final String SELECT_ACOTADO = """
            SELECT a.id
            FROM subscription_amendments a
            WHERE a.id = :amendmentId
              AND a.company_id = :companyId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean existsInCompany(Long amendmentId, Long companyId) {
        if (amendmentId == null || companyId == null)
            return false;
        List<?> filas = entityManager.createNativeQuery(SELECT_ACOTADO)
                .setParameter("amendmentId", amendmentId).setParameter("companyId", companyId)
                .setMaxResults(1).getResultList();
        return !filas.isEmpty();
    }
}
