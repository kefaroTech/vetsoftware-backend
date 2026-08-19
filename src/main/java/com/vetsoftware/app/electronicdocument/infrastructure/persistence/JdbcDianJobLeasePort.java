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
 * Lease sobre {@code electronic_documents} con {@code FOR UPDATE SKIP LOCKED}.
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
     *
     * <p>
     * <strong>Bloqueo optimista: {@code E6_YA_PROTEGIDO} — exención deliberada, no
     * un olvido.</strong> {@code electronic_documents} es una tabla versionada y
     * este {@code UPDATE} la muta, así que por la regla de BE-53 —todo UPDATE
     * nativo sobre tabla versionada mueve también {@code version}— entraría. No lo
     * hace, y es la única excepción viva a esa regla en todo el árbol. Tres
     * razones, y hacen falta las tres:
     * <ul>
     * <li><strong>El mecanismo que ya lo protege es más fuerte.</strong> Las filas
     * se toman con {@code SELECT … FOR UPDATE SKIP LOCKED}: quedan serializadas por
     * un lock pesimista sostenido hasta el commit, que es justo la garantía que el
     * bloqueo optimista aproxima. Añadir {@code version} no cerraría ninguna
     * carrera que este lock deje abierta.</li>
     * <li><strong>{@code dian_leased_until} no es estado de negocio.</strong> Es
     * metadato de coordinación entre trabajos —quién reclamó qué y hasta cuándo—,
     * ni se mapea en {@code ElectronicDocumentJpaEntity} ni viaja al dominio, así
     * que ningún {@code save} cargado antes puede pisarlo: no hay nada que un
     * mapper pueda reescribir desde una copia obsoleta. El defecto que la regla
     * previene no tiene por dónde ocurrir aquí.</li>
     * <li><strong>Subir la versión rompería algo que hoy funciona.</strong> Un
     * {@code updateDianResult} concurrente sobre el mismo documento —en mitad de
     * una transmisión— vería su versión invalidada por un mero renovar del lease y
     * moriría con un 409 {@code CONCURRENT_MODIFICATION} espurio, sobre un
     * conflicto que no existe.</li>
     * </ul>
     * Es el mismo razonamiento que dejó fuera a {@code numbering_resolutions} en
     * BE-26. Y hay un motivo extra para que quede escrito justo aquí: al ir por
     * {@code JdbcTemplate} crudo y no por una {@code @Query} de Spring Data, esta
     * sentencia es <em>invisible</em> a cualquier regla de ArchUnit que escanee
     * anotaciones. Nadie la va a marcar; su única defensa contra un «se nos olvidó»
     * futuro es este párrafo.
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
