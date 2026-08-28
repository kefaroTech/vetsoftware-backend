package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionItemBillingProfile;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lee la linea del contrato del slice {@code subscription} <b>siempre acotada
 * por empresa</b>, y trae con ella su modo de cobro.
 *
 * <p>
 * <b>Por que una consulta nativa y no el repositorio Spring Data del otro
 * slice.</b> Mismo criterio que {@link JpaSubscriptionQueryPort}: aqui hace
 * falta un escalar --{@code charge_mode}-- del par {@code (company_id, id)}, y
 * resolverlo contra la tabla en vez de contra la entidad ajena mantiene el
 * acoplamiento en lo minimo: la forma interna de
 * {@code SubscriptionItemJpaEntity} puede cambiar sin arrastrar a la capa de
 * dinero. El nombre de la tabla y de sus columnas son normativos
 * ({@code suscripciones-tablas.md}).
 *
 * <p>
 * <b>La consulta replica la FK compuesta, ni mas ni menos.</b>
 * {@code fk_subscription_charges_item} referencia
 * {@code subscription_items(company_id, id)} y no mira {@code enabled}; anadir
 * aqui ese filtro rechazaria cierres que la base acepta, que seria cambiar el
 * comportamiento en vez de mejorar el mensaje de error.
 *
 * <p>
 * <b>El {@code charge_mode} se proyecta, no se filtra en el {@code WHERE}</b>,
 * y la diferencia es el mensaje. Un {@code AND i.charge_mode = 'PAID'}
 * convertiria la linea en prueba en "no existe", y el operador que devenga
 * contra ella buscaria un id que esta perfectamente bien. Devolviendo el modo,
 * el caso de uso puede decir en que modo esta. El filtro duro si va en la
 * consulta que <b>selecciona</b> lo que se cobra
 * --{@code SubscriptionChargeJpaRepository} {@code #findPendingForPeriod}--,
 * donde no hay nadie a quien explicarle nada.
 *
 * <p>
 * Es una lectura: no la toca {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}, que
 * solo mira {@code UPDATE}/{@code DELETE}.
 */
@Component
public class JpaSubscriptionItemValidationPort implements SubscriptionItemValidationPort {

    private static final String SELECT_ACOTADO = """
            SELECT i.charge_mode
            FROM subscription_items i
            WHERE i.id = :subscriptionItemId
              AND i.company_id = :companyId
            """;

    /**
     * La misma fila que {@link #SELECT_ACOTADO} con las dos columnas fiscales. Se
     * mantiene como consulta aparte —y no como una sola que sirva a los dos
     * metodos— porque el alta general no debe empezar a validar la coherencia
     * fiscal de una linea que hoy no mira.
     */
    private static final String SELECT_PERFIL_ACOTADO = """
            SELECT i.charge_mode, i.tax_rate, i.tax_treatment
            FROM subscription_items i
            WHERE i.id = :subscriptionItemId
              AND i.company_id = :companyId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ItemChargeMode> findChargeModeInCompany(Long subscriptionItemId,
            Long companyId) {
        if (subscriptionItemId == null || companyId == null)
            return Optional.empty();
        List<?> filas = entityManager.createNativeQuery(SELECT_ACOTADO)
                .setParameter("subscriptionItemId", subscriptionItemId)
                .setParameter("companyId", companyId).setMaxResults(1).getResultList();
        if (filas.isEmpty())
            return Optional.empty();
        return Optional.of(ItemChargeMode.de((String) filas.getFirst()));
    }

    /**
     * <b>El impuesto de la linea, que es el que hereda el excedente.</b> Sin esta
     * consulta el cargo por excedente se construia con {@code EXCLUDED} y tarifa
     * cero fijas: una linea gravada al 19 % generaba un excedente sin IVA, es decir
     * una factura emitida de menos ante la DIAN.
     *
     * <p>
     * <b>Las tres columnas se leen de {@code subscription_items} y no del
     * limite.</b> {@code subscription_item_limits} guarda el precio por unidad del
     * excedente, pero no su fiscalidad, y es correcto que no la guarde: el
     * excedente no es un articulo distinto del contratado. Es la misma fuente de la
     * que ya bebe el motor recurrente en {@link JpaBillableSubscriptionItemPort},
     * para que el cargo del mes y el del exceso no puedan declarar impuestos
     * distintos sobre el mismo articulo.
     */
    @Override
    public Optional<SubscriptionItemBillingProfile> findBillingProfileInCompany(
            Long subscriptionItemId, Long companyId) {
        if (subscriptionItemId == null || companyId == null)
            return Optional.empty();
        List<?> filas = entityManager.createNativeQuery(SELECT_PERFIL_ACOTADO)
                .setParameter("subscriptionItemId", subscriptionItemId)
                .setParameter("companyId", companyId).setMaxResults(1).getResultList();
        if (filas.isEmpty())
            return Optional.empty();
        Object[] fila = (Object[]) filas.getFirst();
        return Optional.of(new SubscriptionItemBillingProfile(ItemChargeMode.de((String) fila[0]),
                (BigDecimal) fila[1], TaxTreatment.valueOf((String) fila[2])));
    }
}
