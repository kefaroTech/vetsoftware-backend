package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.command.SuppressProposalDataCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalSuppressionDto;
import com.vetsoftware.app.aiproposal.application.port.in.SuppressProposalDataUseCase;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Borra lo que el titular pidio borrar.
 *
 * <p>
 * <strong>Sin {@code @Transactional} aqui</strong>: la transaccion la abre el
 * adaptador, que es quien tiene que garantizar que los tres pasos -motivos,
 * turnos y correo- se aplican juntos. Anotarlo tambien aqui no añadiria nada y
 * escondria donde esta de verdad la frontera.
 *
 * <p>
 * <strong>No registra el correo en ningun log.</strong> El evento util es "hubo
 * una supresion y movio N filas"; escribir de quien era en un canal con 31 dias
 * de retencion es reintroducir el dato que se acaba de borrar.
 */
@Observed(name = "aiproposal.suppression")
@Service
public class SuppressProposalDataService implements SuppressProposalDataUseCase {

    private final ProposalRetentionPort retention;

    private final Clock clock;

    public SuppressProposalDataService(ProposalRetentionPort retention, Clock clock) {
        this.retention = retention;
        this.clock = clock;
    }

    @Override
    public ProposalSuppressionDto execute(SuppressProposalDataCommand command) {
        return ProposalSuppressionDto.from(
                retention.suppressByContactEmail(command.contactEmail(), LocalDateTime.now(clock)));
    }
}
