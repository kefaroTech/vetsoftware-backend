package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.subscriptionbilling.domain.BillingCycleSubscription;
import java.time.LocalDate;
import java.util.List;

/**
 * Los contratos a los que hoy les toca cobro, <b>de todas las empresas</b>.
 *
 * <p>
 * <b>No declara ninguna variante acotada por empresa, y eso es lo que lo
 * clasifica.</b> Es un barrido de plataforma: su llamador es un puerto de
 * entrada cerrado a {@code hasRole('SYSTEM')} a secas y no hay ningún camino de
 * tenant que llegue aquí. Añadirle un {@code findDueByCompanyId} sería abrir
 * ese camino sin darse cuenta.
 *
 * <p>
 * <b>El filtro NO mira el estado del contrato</b> (R-TRIAL-13): quien decide si
 * algo devenga es el modo de cobro de cada línea, y un {@code TRIALING} con
 * líneas {@code PAID} tiene que entrar en el barrido.
 */
public interface DueSubscriptionQueryPort {

    /**
     * La página de contratos vivos cuyo próximo cobro cae en {@code runDate} o
     * antes, ordenada por id para que el cursor sea estable.
     *
     * @param afterId
     *            cursor: se devuelven los contratos con id estrictamente mayor
     */
    List<BillingCycleSubscription> dueForBillingAfter(LocalDate runDate, long afterId,
            int batchSize);
}
