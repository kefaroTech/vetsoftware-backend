package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.infrastructure.persistence.AiProposalJpaRepository;
import com.vetsoftware.app.quote.application.port.out.ProposalReferencePort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico fichero de la rodaja {@code quote} que conoce las propuestas del
 * asistente. Es el cruce que el vertical slicing permite: adaptador de
 * persistencia contra el {@code JpaRepository} de la otra feature.
 *
 * <p>
 * <strong>Solo devuelve el id.</strong> No trae la propuesta, ni sus lineas, ni
 * su correo de contacto: esta rodaja no necesita nada de eso y traerlo cruzaria
 * datos personales de un prospecto a un documento comercial sin ninguna razon.
 */
@Component
public class JpaProposalReferencePort implements ProposalReferencePort {

    private final AiProposalJpaRepository aiProposalJpaRepository;

    public JpaProposalReferencePort(AiProposalJpaRepository aiProposalJpaRepository) {
        this.aiProposalJpaRepository = aiProposalJpaRepository;
    }

    @Override
    public Optional<Long> findIdByPublicToken(String publicToken) {
        if (publicToken == null || publicToken.isBlank())
            return Optional.empty();
        return aiProposalJpaRepository.findByPublicToken(publicToken)
                .map(propuesta -> propuesta.getId());
    }
}
