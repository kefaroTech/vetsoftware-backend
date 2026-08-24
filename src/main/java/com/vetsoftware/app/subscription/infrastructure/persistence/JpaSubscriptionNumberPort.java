package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reserva los consecutivos {@code SUS-AAAA-NNNNN} y {@code AMD-AAAA-NNNNN} de
 * forma atomica.
 *
 * <p>
 * <b>Por que no es un «maximo mas uno».</b> Leer el ultimo numero del ano con
 * {@code SELECT ... FOR UPDATE} y sumarle uno serializa a los concurrentes solo
 * <i>si la fila existe</i>. El 1 de enero no existe: ese dia el bloqueo no
 * bloquea nada, dos altas simultaneas calculan el mismo {@code SUS-2027-00001}
 * y la segunda se lleva un 500 por violacion de
 * {@code uq_subscriptions_number}. Peor momento imposible y practicamente
 * irreproducible despues. Es el defecto que hundio al consecutivo de
 * cotizaciones y contra el que advierte la ficha 19 de la especificacion.
 *
 * <p>
 * <b>Como se reserva.</b> Una sola sentencia que <i>crea o incrementa</i>:
 * {@code INSERT ... ON DUPLICATE KEY UPDATE} contra
 * {@code uq_billing_document_sequences_prefix}. Las dos ramas hacen lo mismo y
 * ninguna lee para escribir despues:
 *
 * <ul>
 * <li><b>No habia fila</b> (primer numero del ano): inserta con
 * {@code next_value = 2} y el numero reservado es el 1.
 * <li><b>Ya habia fila</b>: la sube a {@code next_value + 1} y el reservado es
 * el valor anterior.
 * </ul>
 *
 * En los dos casos <b>InnoDB deja la fila bloqueada en exclusiva</b> hasta el
 * commit: si dos transacciones entran a la vez con el ano vacio, una gana el
 * INSERT y la otra espera en la comprobacion de clave duplicada y despues
 * aplica su incremento. La lectura posterior no es una carrera nueva porque
 * solo puede verla quien ya tiene el candado, y una transaccion siempre ve su
 * propia escritura; por eso {@code next_value - 1} es, sin ambiguedad, el
 * numero que acaba de reservar esta transaccion y nadie mas.
 *
 * <p>
 * <b>Todo dentro de la transaccion de negocio</b>, nunca en un
 * {@code REQUIRES_NEW}: si el alta falla, la reserva se deshace con ella y no
 * queda un hueco en la serie.
 *
 * <p>
 * <b>Se reutiliza {@code billing_document_sequences}</b>, el contador de
 * plataforma, con las series {@code SUS-<ano>} y {@code AMD-<ano>} como
 * {@code prefix} —8 caracteres en una columna de 10, sin ningun {@code CHECK}
 * que enumere prefijos validos—. Es el mismo cableado que ya usa el consecutivo
 * de cotizaciones con {@code COT-<ano>}: la tabla no tiene tenant, no va
 * versionada, y conviene leer su ficha como el contador «de documentos de
 * plataforma» y no solo «de las cuentas de cobro», que es como nacio.
 *
 * <p>
 * SQL nativo, igual que el adaptador gemelo de {@code quote}: se ata al
 * esquema, que es el contrato firmado. {@code ON DUPLICATE KEY UPDATE} tampoco
 * es expresable en JPQL.
 */
@Component("subscriptionJpaSubscriptionNumberPort")
public class JpaSubscriptionNumberPort implements SubscriptionNumberPort {

    /** Serie del contrato. Con el ano detras cabe en el VARCHAR(10) del prefijo. */
    private static final String SUBSCRIPTION_SERIES = "SUS-";

    /** Serie del otrosi. */
    private static final String AMENDMENT_SERIES = "AMD-";

    private static final int SEQUENCE_DIGITS = 5;

    private final EntityManager entityManager;

    public JpaSubscriptionNumberPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public String nextSubscriptionNumber(int year) {
        return next(SUBSCRIPTION_SERIES + year);
    }

    @Override
    public String nextAmendmentNumber(int year) {
        return next(AMENDMENT_SERIES + year);
    }

    private String next(String prefix) {
        reserve(prefix);
        return prefix + "-" + String.format("%0" + SEQUENCE_DIGITS + "d", reservedValue(prefix));
    }

    /**
     * Crea la fila del ano o la incrementa, en una sola sentencia. No hay ninguna
     * rama que lea un maximo: el motor decide cual de las dos aplica y, en las dos,
     * la fila queda bloqueada en exclusiva hasta el commit.
     */
    private void reserve(String prefix) {
        entityManager.createNativeQuery("""
                INSERT INTO billing_document_sequences (prefix, next_value)
                VALUES (:prefix, 2)
                ON DUPLICATE KEY UPDATE next_value = next_value + 1
                """).setParameter("prefix", prefix).executeUpdate();
    }

    /**
     * El numero reservado por ESTA transaccion. Se lee sin {@code FOR UPDATE} a
     * proposito: el candado ya lo puso {@link #reserve}, asi que volver a pedirlo
     * no anadiria ninguna garantia y solo sugeriria que la lectura es un paso
     * independiente, que es justo lo que no es.
     */
    private long reservedValue(String prefix) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT next_value - 1
                  FROM billing_document_sequences
                 WHERE prefix = :prefix
                """).setParameter("prefix", prefix).setMaxResults(1).getResultList();
        if (rows.isEmpty()) {
            // Inalcanzable: la sentencia anterior garantiza la fila. Si aparece, es que
            // alguien borro el contador entre las dos, y arrancar la serie desde cero
            // seria mucho peor que fallar aqui.
            throw new IllegalStateException(
                    "Subscription number sequence vanished after reserving it: " + prefix);
        }
        return ((Number) rows.get(0)).longValue();
    }
}
