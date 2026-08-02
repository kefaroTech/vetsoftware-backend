package com.vetsoftware.app.infrastructure.audit.chain;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Acceso a la cadena de hash de la outbox de auditoría. */
@Repository
@ConditionalOnProperty(prefix = "vetsoftware.audit.outbox", name = "enabled", havingValue = "true")
public class AuditChainRepository {

  private final JdbcTemplate jdbcTemplate;

  AuditChainRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Asigna posición y eslabón a las filas todavía sin secuenciar, en orden de inserción.
   *
   * <p>Bloquea la fila única de {@code audit_chain_head} para que varias réplicas no bifurquen la
   * cadena. La transacción es corta y propia del secuenciador: este bloqueo nunca se toma dentro de
   * una transacción de negocio, así que no serializa el tráfico de escritura de la aplicación.
   *
   * @return cuántas filas quedaron secuenciadas
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int sequencePending(int limit, Instant now) {
    Head head = lockHead();

    List<Pending> pending =
        jdbcTemplate.query(
            """
            SELECT id, payload_hash
              FROM audit_event_outbox
             WHERE chain_sequence IS NULL
             ORDER BY id
             LIMIT ?
            """,
            (resultSet, row) ->
                new Pending(resultSet.getLong("id"), resultSet.getString("payload_hash")),
            limit);

    if (pending.isEmpty()) {
      return 0;
    }

    long sequence = head.lastSequence();
    String previousHash = head.lastChainHash();
    List<Object[]> updates = new ArrayList<>(pending.size());

    for (Pending row : pending) {
      sequence++;
      String chainHash = AuditChainHash.chainHash(previousHash, sequence, row.payloadHash());
      updates.add(new Object[] {sequence, previousHash, chainHash, row.id()});
      previousHash = chainHash;
    }

    jdbcTemplate.batchUpdate(
        """
        UPDATE audit_event_outbox
           SET chain_sequence = ?, previous_hash = ?, chain_hash = ?
         WHERE id = ? AND chain_sequence IS NULL
        """,
        updates);

    jdbcTemplate.update(
        """
        UPDATE audit_chain_head
           SET last_sequence = ?, last_chain_hash = ?, updated_at = ?
         WHERE id = 1
        """,
        sequence,
        previousHash,
        Timestamp.from(now));

    return pending.size();
  }

  /** Estado actual de la cabeza. No bloquea. */
  public Head head() {
    return jdbcTemplate.queryForObject(
        """
        SELECT last_sequence, last_chain_hash, last_checkpoint_sequence
          FROM audit_chain_head
         WHERE id = 1
        """,
        (resultSet, row) ->
            new Head(
                resultSet.getLong("last_sequence"),
                resultSet.getString("last_chain_hash"),
                resultSet.getLong("last_checkpoint_sequence")));
  }

  /**
   * Registra hasta dónde quedó anclada la cadena en almacenamiento inmutable. Solo avanza: un
   * checkpoint más antiguo no puede retroceder la marca.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markCheckpoint(long sequence, Instant now) {
    jdbcTemplate.update(
        """
        UPDATE audit_chain_head
           SET last_checkpoint_sequence = ?, updated_at = ?
         WHERE id = 1 AND last_checkpoint_sequence < ?
        """,
        sequence,
        Timestamp.from(now),
        sequence);
  }

  /** Eslabones a partir de {@code afterSequence}, en orden, para recalcular la cadena. */
  public List<Link> linksAfter(long afterSequence, int limit) {
    return jdbcTemplate.query(
        """
        SELECT chain_sequence, payload, payload_hash, previous_hash, chain_hash
          FROM audit_event_outbox
         WHERE chain_sequence IS NOT NULL AND chain_sequence > ?
         ORDER BY chain_sequence
         LIMIT ?
        """,
        (resultSet, row) ->
            new Link(
                resultSet.getLong("chain_sequence"),
                resultSet.getString("payload"),
                resultSet.getString("payload_hash"),
                resultSet.getString("previous_hash"),
                resultSet.getString("chain_hash")),
        afterSequence,
        limit);
  }

  /**
   * Posición más baja que todavía se conserva en la tabla. La depuración elimina eslabones ya
   * publicados y anclados, así que la verificación local arranca en este punto y lo anterior solo
   * es auditable contra el archivo inmutable.
   */
  public long minRetainedSequence() {
    Long value =
        jdbcTemplate.queryForObject(
            "SELECT MIN(chain_sequence) FROM audit_event_outbox WHERE chain_sequence IS NOT NULL",
            Long.class);
    return value == null ? 0 : value;
  }

  public long unsequencedCount() {
    Long value =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_event_outbox WHERE chain_sequence IS NULL", Long.class);
    return value == null ? 0 : value;
  }

  private Head lockHead() {
    return jdbcTemplate.queryForObject(
        """
        SELECT last_sequence, last_chain_hash, last_checkpoint_sequence
          FROM audit_chain_head
         WHERE id = 1
         FOR UPDATE
        """,
        (resultSet, row) ->
            new Head(
                resultSet.getLong("last_sequence"),
                resultSet.getString("last_chain_hash"),
                resultSet.getLong("last_checkpoint_sequence")));
  }

  public record Head(long lastSequence, String lastChainHash, long lastCheckpointSequence) {}

  public record Link(
      long sequence, String payload, String payloadHash, String previousHash, String chainHash) {}

  private record Pending(long id, String payloadHash) {}
}
