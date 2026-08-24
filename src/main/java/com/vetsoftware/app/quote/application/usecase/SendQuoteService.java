package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.SendQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DRAFT -> SENT. A partir de aqui la oferta salio y sus lineas no se tocan. */
@Observed(name = "quote.send")
@Service
public class SendQuoteService implements SendQuoteUseCase {

    private final QuoteRepository repository;
    private final Clock clock;

    public SendQuoteService(QuoteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public QuoteDto execute(SendQuoteCommand command) {
        Quote quote = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new QuoteNotFoundException(command.id()));
        quote.send(LocalDate.now(clock));
        return QuoteDto.from(repository.save(quote));
    }
}
