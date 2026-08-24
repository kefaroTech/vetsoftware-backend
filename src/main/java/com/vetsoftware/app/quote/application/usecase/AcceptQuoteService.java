package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.AcceptQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SENT -> ACCEPTED, sellando la prueba de la aceptacion.
 *
 * <p>
 * El reloj se inyecta: la fecha de aceptacion es la prueba, y una prueba que
 * depende del reloj de la maquina no se puede fijar en un test.
 */
@Observed(name = "quote.accept")
@Service
public class AcceptQuoteService implements AcceptQuoteUseCase {

    private final QuoteRepository repository;
    private final Clock clock;

    public AcceptQuoteService(QuoteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public QuoteDto execute(AcceptQuoteCommand command) {
        Quote quote = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new QuoteNotFoundException(command.id()));
        LocalDateTime now = LocalDateTime.now(clock);
        quote.accept(command.acceptedByEmail(), command.acceptedIp(), now, now.toLocalDate());
        return QuoteDto.from(repository.save(quote));
    }
}
