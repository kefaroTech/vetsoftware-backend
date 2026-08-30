package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.port.in.MarkProposalConvertedUseCase;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.ProposalStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe {@code CONVERTED} pasando por el dominio.
 *
 * <p>
 * <strong>Por que por el dominio y no con un {@code UPDATE}.</strong>
 * {@code AiProposal.marcarConvertida} ademas mueve {@code lastActivityAt}, y
 * esa fecha es la que decide la purga de retencion: un {@code UPDATE} directo
 * dejaria la propuesta marcada como convertida y con la ultima actividad vieja,
 * o sea candidata a purga el mismo dia en que se convirtio. Hasta hoy este
 * metodo del dominio no lo llamaba nadie —era codigo muerto— y el estado
 * {@code CONVERTED} no lo escribia nada.
 */
@Observed(name = "aiproposal.convert")
@Service
public class MarkProposalConvertedService implements MarkProposalConvertedUseCase {

    private final AiProposalRepository repository;
    private final Clock clock;

    public MarkProposalConvertedService(AiProposalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<Long> execute(String publicToken) {
        if (publicToken == null || publicToken.isBlank())
            return Optional.empty();
        Optional<AiProposal> encontrada = repository.findByPublicToken(publicToken);
        if (encontrada.isEmpty())
            return Optional.empty();
        AiProposal propuesta = encontrada.get();
        // Idempotente: una propuesta ya convertida devuelve su id sin reescribir su
        // ultima actividad, que la volveria a alejar de la purga sin motivo.
        if (propuesta.getStatus() == ProposalStatus.CONVERTED)
            return Optional.of(propuesta.getId());
        propuesta.marcarConvertida(clock);
        return Optional.of(repository.save(propuesta).getId());
    }
}
