package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.BillableSubscriptionItemPort;
import com.vetsoftware.app.subscriptionbilling.domain.BillableSubscriptionItem;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Las lineas del contrato vigentes un dia, con su modo de cobro y su tarifa.
 *
 * <p>
 * <b>Consulta nativa por la tabla</b>, mismo criterio que
 * {@link JpaSubscriptionItemValidationPort}.
 *
 * <p>
 * <b>El {@code charge_mode} se proyecta, no se filtra.</b> Es la misma decision
 * —y por el mismo motivo— que en el puerto de validacion: la regla que decide
 * quien devenga tiene que vivir en un sitio que se pueda probar sin base de
 * datos, y la escribe {@code BillableSubscriptionItem#devenga}. Ademas, una
 * linea {@code TRIAL} <b>conserva su tarifa real</b> (R-TRIAL-14), asi que una
 * consulta que la trajera sin su modo no devolveria ceros: devolveria el precio
 * completo y se lo cobraria a todos los clientes en prueba.
 *
 * <p>
 * <b>{@code enabled = TRUE} no es ceremonia: es la coherencia con las otras
 * tres consultas de esta misma tabla.</b> {@code ContractItemJpaRepository}
 * (permisos) y {@code EffectiveLimitCandidateJpaRepository} (cupos) lo llevan
 * las dos, asi que una linea dada de baja logica deja de conceder submodulos y
 * deja de conceder cantidad. Sin este filtro seguia <b>devengando</b>: al
 * cliente se le retiraba lo contratado y se le seguia facturando, y nada en el
 * sistema decia que las tres consultas hubieran dejado de hablar de la misma
 * poblacion.
 *
 * <p>
 * <b>Vigencia semiabierta {@code [effective_from, effective_to)}</b>, igual que
 * {@code SubscriptionItemJpaRepository#findAllCurrentOn}: el dia en que una
 * linea se cierra y su sucesora se abre pertenece a la sucesora, y solo a ella.
 * Con el extremo cerrado se cobrarian las dos ese dia.
 *
 * <p>
 * Es una lectura acotada por empresa: no la toca
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} y satisface
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}.
 */
@Component
public class JpaBillableSubscriptionItemPort implements BillableSubscriptionItemPort {

    private static final String SELECT_VIGENTES = """
            SELECT i.id, i.company_id, i.subscription_id, i.catalog_item_id, i.item_name,
                   i.charge_mode, i.quantity, i.included_quantity, i.unit_amount, i.tax_rate,
                   i.tax_treatment, i.effective_from, i.effective_to
            FROM subscription_items i
            WHERE i.company_id = :companyId
              AND i.subscription_id = :subscriptionId
              AND i.enabled = TRUE
              AND i.effective_from <= :day
              AND (i.effective_to IS NULL OR i.effective_to > :day)
            ORDER BY i.id ASC
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<BillableSubscriptionItem> findCurrentOn(Long companyId, Long subscriptionId,
            LocalDate day) {
        if (companyId == null || subscriptionId == null || day == null)
            return List.of();
        List<?> filas = entityManager.createNativeQuery(SELECT_VIGENTES)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("day", day).getResultList();
        List<BillableSubscriptionItem> lineas = new ArrayList<>(filas.size());
        for (Object cruda : filas) {
            Object[] fila = (Object[]) cruda;
            lineas.add(new BillableSubscriptionItem(((Number) fila[0]).longValue(),
                    ((Number) fila[1]).longValue(), ((Number) fila[2]).longValue(),
                    fila[3] == null ? null : ((Number) fila[3]).longValue(), (String) fila[4],
                    ItemChargeMode.de((String) fila[5]), ((Number) fila[6]).intValue(),
                    ((Number) fila[7]).intValue(), (BigDecimal) fila[8], (BigDecimal) fila[9],
                    TaxTreatment.valueOf((String) fila[10]), SqlDates.toLocalDate(fila[11]),
                    SqlDates.toLocalDate(fila[12])));
        }
        return List.copyOf(lineas);
    }
}
