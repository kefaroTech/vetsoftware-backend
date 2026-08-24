package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Comprueba la linea del contrato del slice {@code subscription} <b>siempre
 * acotada por empresa</b>.
 *
 * <p>
 * <b>Por que una consulta nativa y no el repositorio Spring Data del otro
 * slice.</b> Mismo criterio que {@link JpaSubscriptionQueryPort}: aqui solo
 * hace falta saber si el par {@code (company_id, id)} existe, y resolverlo
 * contra la tabla en vez de contra la entidad ajena mantiene el acoplamiento en
 * lo minimo —la forma interna de {@code SubscriptionItemJpaEntity} puede
 * cambiar sin arrastrar a la capa de dinero—. El nombre de la tabla y de sus
 * dos columnas son normativos ({@code suscripciones-tablas.md}).
 *
 * <p>
 * <b>La consulta replica la FK compuesta, ni mas ni menos.</b>
 * {@code fk_subscription_charges_item} referencia
 * {@code subscription_items(company_id, id)} y no mira {@code enabled}; anadir
 * aqui ese filtro rechazaria cierres que la base acepta, que seria cambiar el
 * comportamiento en vez de mejorar el mensaje de error.
 *
 * <p>
 * Es una lectura: no la toca {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}, que
 * solo mira {@code UPDATE}/{@code DELETE}.
 */
@Component
public class JpaSubscriptionItemValidationPort implements SubscriptionItemValidationPort {

    private static final String SELECT_ACOTADO = """
            SELECT i.id
            FROM subscription_items i
            WHERE i.id = :subscriptionItemId
              AND i.company_id = :companyId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean existsInCompany(Long subscriptionItemId, Long companyId) {
        if (subscriptionItemId == null || companyId == null)
            return false;
        List<?> filas = entityManager.createNativeQuery(SELECT_ACOTADO)
                .setParameter("subscriptionItemId", subscriptionItemId)
                .setParameter("companyId", companyId).setMaxResults(1).getResultList();
        return !filas.isEmpty();
    }
}
