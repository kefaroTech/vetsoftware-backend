package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpayment.application.port.out.CustomerCreditQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.CustomerCreditLotRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lee el lote de saldo a favor del slice {@code customercredit} <b>siempre
 * acotado por empresa y siempre con la fila bloqueada</b>.
 *
 * <p>
 * <b>El {@code entry_kind = 'GRANT'} va en el {@code WHERE} y no se
 * proyecta</b>, al reves que el {@code charge_mode} de una linea de contrato.
 * La diferencia es que aqui no hay nada que explicarle a nadie: un consumo o
 * una caducidad <em>no son</em> un lote del que se pueda gastar, y devolverlos
 * para que el caso de uso los rechazara solo abriria la puerta a que alguien se
 * olvidara de hacerlo. Los otros dos tipos de asiento restan; aplicarlos como
 * origen seria gastar dos veces el mismo dinero.
 *
 * <p>
 * <b>{@code FOR UPDATE} sobre una consulta nativa, y sin
 * {@code setMaxResults}.</b> El filtro es la clave primaria, asi que devuelve
 * una fila como maximo y el limite sobra; ponerlo ademas romperia la sentencia,
 * porque Hibernate anade su {@code limit ?} <em>al final</em> —detras del
 * {@code FOR UPDATE}— y MySQL exige el orden contrario.
 *
 * <p>
 * Es una lectura: no la toca {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}. Que
 * tome candado no la convierte en mutacion, pero el {@code company_id} del
 * {@code WHERE} sigue siendo obligatorio por el mismo motivo que alli — un
 * candado concedido sobre la fila de otro tenant ya se concedio, aunque despues
 * revierta.
 */
@Component
public class JpaCustomerCreditQueryPort implements CustomerCreditQueryPort {

    private static final String SELECT_ACOTADO_CON_CANDADO = """
            SELECT e.id, e.company_id, e.amount, e.expires_on
            FROM customer_credit_entries e
            WHERE e.id = :creditEntryId
              AND e.company_id = :companyId
              AND e.entry_kind = 'GRANT'
            FOR UPDATE
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<CustomerCreditLotRef> lockLotByIdAndCompanyId(Long creditEntryId,
            Long companyId) {
        if (creditEntryId == null || companyId == null)
            return Optional.empty();
        List<?> filas = entityManager.createNativeQuery(SELECT_ACOTADO_CON_CANDADO)
                .setParameter("creditEntryId", creditEntryId).setParameter("companyId", companyId)
                .getResultList();
        if (filas.isEmpty())
            return Optional.empty();
        Object[] fila = (Object[]) filas.get(0);
        return Optional.of(new CustomerCreditLotRef(((Number) fila[0]).longValue(),
                ((Number) fila[1]).longValue(), (BigDecimal) fila[2], toLocalDate(fila[3])));
    }

    /**
     * Segun el driver, una columna {@code DATE} de una consulta nativa llega como
     * {@link Date} o ya como {@link LocalDate}. Aceptar las dos formas evita un
     * fallo que solo aparece en el entorno donde no estas mirando.
     */
    private static LocalDate toLocalDate(Object value) {
        if (value == null)
            return null;
        if (value instanceof LocalDate fecha)
            return fecha;
        if (value instanceof Date fecha)
            return fecha.toLocalDate();
        if (value instanceof java.util.Date fecha)
            return new Date(fecha.getTime()).toLocalDate();
        throw new IllegalStateException(
                "No se pudo leer expires_on de la consulta nativa: " + value.getClass().getName());
    }
}
