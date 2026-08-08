package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lease sobre {@code electronic_documents} con {@code FOR UPDATE SKIP LOCKED},
 * el mismo mecanismo que {@code AuditOutboxRepository} usa para el outbox de
 * auditoría.
 *
 * <p>
 * {@code SKIP LOCKED} es lo que hace que las réplicas no se estorben: en vez de
 * esperar a que se libere una fila que otra transacción ya bloqueó, la salta y
 * sigue con la siguiente. Cada réplica termina con un lote disjunto sin
 * necesidad de coordinarse.
 *
 * <p>
 * Se escribe con {@code JdbcTemplate} y no con JPA porque {@code SKIP LOCKED}
 * no se expresa en JPQL, y porque {@code dian_leased_until} es puro andamiaje
 * de los jobs: no forma parte del documento como concepto de negocio y no se
 * mapea en la entidad.
 */
@Component
public class JdbcDianJobLeasePort implements DianJobLeasePort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDianJobLeasePort(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Las dos sentencias van en la misma transacción a propósito: el
     * {@code SELECT … FOR UPDATE} mantiene el bloqueo sobre las filas elegidas
     * hasta el commit, así que ninguna otra réplica puede reclamarlas entre la
     * lectura y la marca.
     */
    @Override
    @Transactional
    public List<Long> leaseByDianStatus(DianStatus status, int limit, Duration lease) {
        Instant now = Instant.now();
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id
                  FROM electronic_documents
                 WHERE dian_status = ?
                   AND (dian_leased_until IS NULL OR dian_leased_until < ?)
                 ORDER BY issue_date
                 LIMIT ?
                 FOR UPDATE SKIP LOCKED
                """, Long.class, status.name(), Timestamp.from(now), limit);

        if (ids.isEmpty()) {
            return List.of();
        }

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        Object[] arguments = Stream.concat(Stream.of(Timestamp.from(now.plus(lease))), ids.stream())
                .toArray();
        jdbcTemplate.update("""
                UPDATE electronic_documents
                   SET dian_leased_until = ?
                 WHERE id IN (%s)
                """.formatted(placeholders), arguments);
        return ids;
    }
}
