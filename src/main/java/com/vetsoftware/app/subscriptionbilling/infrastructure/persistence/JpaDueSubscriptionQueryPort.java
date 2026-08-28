package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.DueSubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.BillingCycleSubscription;
import com.vetsoftware.app.subscriptionbilling.domain.BillingPeriodicity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los contratos a los que hoy les toca cobro, de todas las clinicas.
 *
 * <p>
 * <b>Consulta nativa por la tabla</b>, mismo criterio que
 * {@link JpaSubscriptionQueryPort} y {@link JpaSubscriptionItemValidationPort}:
 * este slice necesita ocho escalares del contrato ajeno y no su entidad, asi
 * que la forma interna de {@code SubscriptionJpaEntity} puede cambiar sin
 * arrastrar a la capa de dinero. Los nombres de tabla y columnas son
 * normativos.
 *
 * <h2>Lo que el WHERE NO dice, que es lo importante</h2>
 *
 * <p>
 * <b>No filtra por estado para decidir si algo se cobra.</b> Los cuatro estados
 * que admite son los <em>vigentes</em> —los mismos de {@code active_marker}—,
 * es decir la pregunta "sigue siendo cliente", no "esta pagando".
 * {@code TRIALING} entra a proposito: un contrato en prueba tiene lineas de
 * pago obligatorio —facturacion electronica DIAN— y dejarlo fuera es dejar de
 * cobrarlas (R-TRIAL-13). Quien decide si una linea devenga es su
 * {@code charge_mode}, y eso se resuelve en
 * {@link JpaBillableSubscriptionItemPort}.
 *
 * <p>
 * <b>El cursor es el id y el orden es total.</b> Sin {@code ORDER BY s.id} el
 * barrido puede repetir u omitir contratos entre paginas, que en un cierre
 * mensual significa facturar dos veces o no facturar.
 *
 * <p>
 * Es una lectura: no la toca {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}, que
 * solo mira {@code UPDATE}/{@code DELETE}. Y no lleva filtro de empresa porque
 * es un barrido de plataforma, cuyo unico consumidor es un puerto cerrado a
 * {@code hasRole('SYSTEM')} a secas.
 */
@Component
public class JpaDueSubscriptionQueryPort implements DueSubscriptionQueryPort {

    private static final String SELECT_VENCIDOS = """
            SELECT s.id, s.company_id, s.billing_cycle, s.start_date, s.trial_end_date,
                   s.current_period_start, s.current_period_end, s.next_billing_date
            FROM subscriptions s
            WHERE s.enabled = true
              AND s.status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
              AND s.id > :afterId
              AND (
                    (s.next_billing_date IS NOT NULL AND s.next_billing_date <= :runDate)
                 OR (s.next_billing_date IS NULL
                     AND COALESCE(DATE_ADD(s.trial_end_date, INTERVAL 1 DAY), s.start_date)
                         <= :runDate)
              )
            ORDER BY s.id ASC
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<BillingCycleSubscription> dueForBillingAfter(LocalDate runDate, long afterId,
            int batchSize) {
        if (runDate == null)
            throw new IllegalArgumentException("runDate is required");
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        List<?> filas = entityManager.createNativeQuery(SELECT_VENCIDOS)
                .setParameter("afterId", afterId).setParameter("runDate", runDate)
                .setMaxResults(batchSize).getResultList();
        List<BillingCycleSubscription> contratos = new ArrayList<>(filas.size());
        for (Object cruda : filas) {
            Object[] fila = (Object[]) cruda;
            contratos.add(new BillingCycleSubscription(((Number) fila[0]).longValue(),
                    ((Number) fila[1]).longValue(), BillingPeriodicity.de((String) fila[2]),
                    SqlDates.toLocalDate(fila[3]), SqlDates.toLocalDate(fila[4]),
                    SqlDates.toLocalDate(fila[5]), SqlDates.toLocalDate(fila[6]),
                    SqlDates.toLocalDate(fila[7])));
        }
        return List.copyOf(contratos);
    }
}
