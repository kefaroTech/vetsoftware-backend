package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.registration.application.port.out.ProposalConversionRecorder;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Escribe la fila del puente propuesta &rarr; empresa.
 *
 * <p>
 * <strong>La entidad vive en esta misma rodaja</strong>, asi que no hay ningun
 * cruce de vertical slicing que justificar: el puente lo escribe el alta y solo
 * el alta. Ver {@link AiProposalConversionJpaEntity} para por que NO puede
 * vivir ni en {@code aiproposal} ni en {@code company}.
 *
 * <p>
 * &#9940; <strong>Comprueba antes de insertar, y eso no es opcional.</strong>
 * La tabla lleva <em>dos</em> unicos —una propuesta convierte una vez, una
 * empresa nace de una sola propuesta— y cualquiera de los dos convertiria un
 * alta perfectamente valida en un 500. El caso real no es raro: dos personas de
 * la misma clinica abren el mismo enlace de la propuesta y se registran.
 *
 * <p>
 * <strong>La comprobacion previa es comodidad, no garantia</strong>, igual que
 * en el resto del repositorio: un {@code SELECT} seguido de un {@code INSERT}
 * es una carrera y la autoridad son los dos unicos. Lo que hace es que el caso
 * frecuente —el reintento— no acabe en error.
 */
@Component
public class JpaProposalConversionRecorder implements ProposalConversionRecorder {

    private final AiProposalConversionJpaRepository repository;
    private final Clock clock;

    public JpaProposalConversionRecorder(AiProposalConversionJpaRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(Long proposalId, Long companyId) {
        if (proposalId == null || companyId == null)
            return;
        if (repository.existsByProposalId(proposalId) || repository.existsByCompanyId(companyId)) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        repository.save(new AiProposalConversionJpaEntity(proposalId, companyId, ahora, ahora));
    }
}
