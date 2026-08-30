package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * La bitacora de supresiones: se inserta y se lee, nunca se actualiza.
 *
 * <p>
 * <strong>No declara ninguna escritura sobre fila existente</strong>, y eso es
 * lo que sostiene la exencion {@code E1_APPEND_ONLY} de la entidad. Un
 * {@code @Modifying} aqui la dejaria en falso.
 */
public interface AiProposalSuppressionRequestJpaRepository
        extends
            JpaRepository<AiProposalSuppressionRequestJpaEntity, Long> {

    /**
     * Cuando se atendio la ultima peticion de este titular, si hubo alguna. Es la
     * consulta para la que el changeset 392 declara
     * {@code ix_ai_proposal_suppression_requests_subject}: filtra por hash y ya
     * llega ordenada por fecha.
     *
     * <p>
     * &#9940; <strong>Se consulta ANTES de insertar la fila nueva.</strong>
     * Despues, la fila recien escrita seria su propia predecesora y toda peticion
     * —incluida la primera— saldria como repetida.
     *
     * <p>
     * Busca por hash y no por correo porque el correo no esta aqui: es la unica
     * llave que sobrevive al borrado, y por eso tiene que calcularse igual que la
     * columna generada de {@code ai_proposals}.
     *
     * <p>
     * Es un {@code SELECT} agregado, no una mutacion: sin {@code @Modifying} y sin
     * {@code version = version + 1} que anadir.
     */
    @Query("select max(r.executedAt) from AiProposalSuppressionRequestJpaEntity r "
            + "where r.subjectEmailHash = :subjectEmailHash")
    Optional<LocalDateTime> findLastExecutedAt(@Param("subjectEmailHash") byte[] subjectEmailHash);
}
