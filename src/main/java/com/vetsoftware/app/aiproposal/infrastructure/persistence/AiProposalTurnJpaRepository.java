package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data sobre {@code ai_proposal_turns}. */
public interface AiProposalTurnJpaRepository extends JpaRepository<AiProposalTurnJpaEntity, Long> {

    /**
     * Los turnos de una propuesta, en el orden en que ocurrieron. El orden es
     * contenido, no presentacion: los refinamientos son acumulativos y leerlos
     * desordenados cambia lo que se reconstruye.
     */
    List<AiProposalTurnJpaEntity> findByProposalIdOrderByTurnNumberAsc(Long proposalId);
}
