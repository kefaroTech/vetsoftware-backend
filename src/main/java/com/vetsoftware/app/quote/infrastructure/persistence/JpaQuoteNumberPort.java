package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Asigna el consecutivo {@code COT-AAAA-NNNNN} reservandolo de forma atomica.
 *
 * <p>
 * <b>Por que no es un "maximo mas uno".</b> La primera version leia la ultima
 * cotizacion del ano con {@code SELECT ... FOR UPDATE} y le sumaba uno. Eso
 * serializa a los concurrentes solo <i>si la fila existe</i>, y el 1 de enero
 * no existe: ese dia el bloqueo no bloquea nada, dos comerciales cotizando a la
 * vez calculan el mismo {@code COT-2027-00001} y el segundo se lleva un 500 por
 * violacion de {@code uq_quotes_number}. Peor momento imposible y practicamente
 * irreproducible despues. Es exactamente el defecto contra el que advierte la
 * ficha 19 de la especificacion al justificar por que el contador es una tabla
 * y no una consulta: <i>"un numero consecutivo no se puede sacar de un maximo
 * mas uno, porque dos procesos simultaneos leerian el mismo maximo y darian el
 * mismo numero a dos documentos distintos"</i>.
 *
 * <p>
 * <b>Como se reserva ahora.</b> Una sola sentencia que <i>crea o
 * incrementa</i>: {@code INSERT ... ON DUPLICATE KEY UPDATE} contra
 * {@code uq_billing_document_sequences_prefix}. Las dos ramas hacen lo mismo y
 * ninguna lee para escribir despues:
 *
 * <ul>
 * <li><b>No habia fila</b> (primer numero del ano): inserta con
 * {@code next_value = 2} y el numero reservado es el 1.
 * <li><b>Ya habia fila</b>: la sube a {@code next_value + 1} y el numero
 * reservado es el valor anterior.
 * </ul>
 *
 * En los dos casos <b>InnoDB deja la fila bloqueada en exclusiva</b> hasta el
 * commit: si dos transacciones entran a la vez con el ano vacio, una gana el
 * INSERT y la otra se queda esperando en la comprobacion de clave duplicada y
 * despues aplica su incremento. La lectura posterior no es una carrera nueva
 * porque solo puede verla quien ya tiene el candado, y una transaccion siempre
 * ve su propia escritura; por eso {@code next_value - 1} es, sin ambiguedad, el
 * numero que acaba de reservar esta transaccion y nadie mas.
 *
 * <p>
 * <b>Todo dentro de la transaccion de negocio</b>, nunca en un
 * {@code REQUIRES_NEW}: si el alta de la cotizacion falla, la reserva se
 * deshace con ella y no queda un hueco en la serie. Es la propiedad que la
 * especificacion da por buena para este contador —<i>"sin carrera y sin huecos,
 * porque el incremento va dentro de la misma transaccion y un fallo lo
 * deshace"</i>— y la diferencia deliberada con el consecutivo fiscal de la
 * DIAN, que si debe conservar el hueco.
 *
 * <p>
 * <b>Se reutiliza {@code billing_document_sequences}</b>, el contador de
 * plataforma, con la serie {@code COT-<ano>} como {@code prefix} —8 caracteres
 * en una columna de 10, y sin ningun {@code CHECK} que enumere prefijos
 * validos—. La tabla no tiene tenant, no va versionada y su unica razon de
 * existir es esta. La ficha 19 la describe hoy como el contador "de las cuentas
 * de cobro" porque nacio para eso: conviene ensanchar esa frase a "documentos
 * de plataforma" para que el proximo que lea la tabla no se sorprenda con las
 * filas {@code COT-}.
 *
 * <p>
 * SQL nativo, como los cuatro adaptadores de catalogo del slice: se ata al
 * esquema, que es el contrato firmado, y no al modelado Java de otra feature.
 * {@code ON DUPLICATE KEY UPDATE} tampoco es expresable en JPQL.
 */
@Component
public class JpaQuoteNumberPort implements QuoteNumberPort {

    /**
     * Serie de las cotizaciones. Con el ano detras cabe en el VARCHAR(10) del
     * prefijo.
     */
    private static final String SERIES = "COT-";

    private static final int SEQUENCE_DIGITS = 5;

    private final EntityManager entityManager;

    public JpaQuoteNumberPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public String next(int year) {
        String prefix = SERIES + year;
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
     * proposito: el candado ya lo tiene puesto {@link #reserve}, asi que volver a
     * pedirlo no anadiria ninguna garantia y solo sugeriria que la lectura es un
     * paso independiente, que es justo lo que no es.
     */
    private long reservedValue(String prefix) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT next_value - 1
                  FROM billing_document_sequences
                 WHERE prefix = :prefix
                """).setParameter("prefix", prefix).setMaxResults(1).getResultList();
        if (rows.isEmpty()) {
            // Inalcanzable: la sentencia anterior garantiza la fila. Si aparece, es
            // que alguien borro el contador entre las dos, y arrancar la serie desde
            // cero seria mucho peor que fallar aqui.
            throw new IllegalStateException(
                    "Quote number sequence vanished after reserving it: " + prefix);
        }
        return ((Number) rows.get(0)).longValue();
    }
}
