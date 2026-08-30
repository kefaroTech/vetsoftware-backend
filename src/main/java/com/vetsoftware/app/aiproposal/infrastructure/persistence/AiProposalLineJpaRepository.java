package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data sobre {@code ai_proposal_lines}. */
public interface AiProposalLineJpaRepository extends JpaRepository<AiProposalLineJpaEntity, Long> {

    /**
     * Las lineas de un turno. El desempate por {@code id} es obligatorio: sin el,
     * dos lineas con el mismo {@code sort_order} pueden salir en distinto orden en
     * dos lecturas, y el orden de la propuesta es lo que ve el cliente.
     */
    List<AiProposalLineJpaEntity> findByTurnIdOrderBySortOrderAscIdAsc(Long turnId);
}
