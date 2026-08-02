package com.vetsoftware.app.infrastructure.audit.outbox;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(
        prefix = "vetsoftware.audit.outbox",
        name = "enabled",
        havingValue = "true")
class AuditOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    AuditOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Reclama filas con un lease; {@code SKIP LOCKED} permite ejecutar varias réplicas sin
     * publicar el mismo lote de forma concurrente.
     */
    @Transactional
    public List<AuditOutboxRecord> claim(int limit, Instant now, Duration leaseDuration) {
        // Solo se publica lo ya secuenciado: el registro que llega al archivo inmutable debe llevar
        // su posición y su eslabón. Lo insertado entre el secuenciado y esta consulta espera al
        // ciclo siguiente.
        List<AuditOutboxRecord> records = jdbcTemplate.query("""
                SELECT id, event_id, payload, attempts, chain_sequence, previous_hash,
                       payload_hash, chain_hash
                  FROM audit_event_outbox
                 WHERE chain_sequence IS NOT NULL
                   AND ((status IN ('PENDING', 'FAILED') AND next_attempt_at <= ?)
                    OR (status = 'PROCESSING' AND locked_until < ?))
                 ORDER BY chain_sequence
                 LIMIT ?
                 FOR UPDATE SKIP LOCKED
                """,
                (resultSet, row) -> new AuditOutboxRecord(
                        resultSet.getLong("id"),
                        resultSet.getString("event_id"),
                        resultSet.getString("payload"),
                        resultSet.getInt("attempts") + 1,
                        resultSet.getLong("chain_sequence"),
                        resultSet.getString("previous_hash"),
                        resultSet.getString("payload_hash"),
                        resultSet.getString("chain_hash")),
                Timestamp.from(now), Timestamp.from(now), limit);

        if (records.isEmpty()) {
            return records;
        }
        Timestamp lockedUntil = Timestamp.from(now.plus(leaseDuration));
        jdbcTemplate.batchUpdate("""
                UPDATE audit_event_outbox
                   SET status = 'PROCESSING', attempts = attempts + 1,
                       locked_until = ?, last_error = NULL
                 WHERE id = ?
                """,
                records,
                records.size(),
                (statement, record) -> {
                    statement.setTimestamp(1, lockedUntil);
                    statement.setLong(2, record.id());
                });
        return records;
    }

    @Transactional
    public void markPublished(List<Long> ids, Instant now) {
        if (ids.isEmpty()) {
            return;
        }
        Timestamp publishedAt = Timestamp.from(now);
        jdbcTemplate.batchUpdate("""
                UPDATE audit_event_outbox
                   SET status = 'PUBLISHED', published_at = ?, locked_until = NULL,
                       next_attempt_at = ?, last_error = NULL
                 WHERE id = ? AND status = 'PROCESSING'
                """,
                ids,
                ids.size(),
                (statement, id) -> {
                    statement.setTimestamp(1, publishedAt);
                    statement.setTimestamp(2, publishedAt);
                    statement.setLong(3, id);
                });
    }

    @Transactional
    public void markFailed(long id, Instant nextAttemptAt, String error) {
        jdbcTemplate.update("""
                UPDATE audit_event_outbox
                   SET status = 'FAILED', next_attempt_at = ?, locked_until = NULL,
                       last_error = ?
                 WHERE id = ? AND status = 'PROCESSING'
                """,
                Timestamp.from(nextAttemptAt), truncate(error), id);
    }

    /**
     * Depura eventos ya publicados. Nunca rebasa la marca de checkpoint: eliminar un eslabón que
     * todavía no quedó anclado en almacenamiento inmutable abriría un hueco imposible de auditar
     * después.
     */
    @Transactional
    public int deletePublishedBefore(Instant cutoff, int limit) {
        return jdbcTemplate.update("""
                DELETE FROM audit_event_outbox
                 WHERE status = 'PUBLISHED'
                   AND published_at < ?
                   AND chain_sequence IS NOT NULL
                   AND chain_sequence <= (
                       SELECT last_checkpoint_sequence FROM audit_chain_head WHERE id = 1)
                 ORDER BY id
                 LIMIT ?
                """,
                Timestamp.from(cutoff), limit);
    }

    public long pendingCount() {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM audit_event_outbox
                 WHERE status IN ('PENDING', 'PROCESSING', 'FAILED')
                """, Long.class);
        return value == null ? 0 : value;
    }

    public long failedCount() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event_outbox WHERE status = 'FAILED'", Long.class);
        return value == null ? 0 : value;
    }

    public double oldestPendingAgeSeconds() {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(created_at), UTC_TIMESTAMP(6)), 0)
                  FROM audit_event_outbox
                 WHERE status IN ('PENDING', 'PROCESSING', 'FAILED')
                """, Long.class);
        return value == null ? 0 : value.doubleValue();
    }

    private static String truncate(String error) {
        String value = error == null || error.isBlank() ? "unknown_error" : error;
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
