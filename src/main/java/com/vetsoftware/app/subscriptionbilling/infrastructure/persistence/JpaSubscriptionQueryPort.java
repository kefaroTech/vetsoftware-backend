package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resuelve el contrato del slice {@code subscription} <b>siempre acotado por
 * empresa</b>.
 *
 * <p>
 * <b>Por qué una consulta nativa y no el repositorio Spring Data del otro
 * slice.</b> Este adaptador solo necesita dos escalares —el id y la empresa—
 * para comprobar que el contrato existe y es de quien dice serlo. Resolverlo
 * con la tabla en vez de con la entidad ajena mantiene el acoplamiento en lo
 * mínimo: la forma interna de {@code SubscriptionJpaEntity} puede cambiar sin
 * arrastrar a la capa de dinero, y este slice no queda atado a qué
 * <em>getters</em> expone. El nombre de la tabla y de sus dos columnas son
 * normativos ({@code suscripciones-tablas.md}, ficha 13).
 *
 * <p>
 * <b>El filtro por {@code company_id} no es defensa en profundidad: es lo que
 * impide colgar un cargo de la clínica A del contrato de la clínica B.</b> La
 * FK compuesta lo rechazaría también en la base, pero con un error de
 * constraint convertido en 500 a mitad del cierre mensual. Aquí devuelve vacío
 * y el caso de uso da un mensaje que dice qué pasó.
 *
 * <p>
 * Es una lectura: no la toca {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}, que
 * solo mira {@code UPDATE}/{@code DELETE}.
 */
@Component
public class JpaSubscriptionQueryPort implements SubscriptionQueryPort {

    private static final String SELECT_ACOTADO = """
            SELECT s.id, s.company_id
            FROM subscriptions s
            WHERE s.id = :subscriptionId
              AND s.company_id = :companyId
              AND s.enabled = true
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<SubscriptionRef> findByIdAndCompanyId(Long subscriptionId, Long companyId) {
        if (subscriptionId == null || companyId == null)
            return Optional.empty();
        List<?> filas = entityManager.createNativeQuery(SELECT_ACOTADO)
                .setParameter("subscriptionId", subscriptionId).setParameter("companyId", companyId)
                .setMaxResults(1).getResultList();
        if (filas.isEmpty())
            return Optional.empty();
        Object[] fila = (Object[]) filas.get(0);
        return Optional.of(new SubscriptionRef(((Number) fila[0]).longValue(),
                ((Number) fila[1]).longValue()));
    }
}
