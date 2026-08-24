package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.RejectQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.RejectQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SENT -> REJECTED. La oferta queda entera; lo que cambia es el desenlace. */
@Observed(name = "quote.reject")
@Service
public class RejectQuoteService implements RejectQuoteUseCase {

    private final QuoteRepository repository;

    public RejectQuoteService(QuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public QuoteDto execute(RejectQuoteCommand command) {
        Quote quote = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new QuoteNotFoundException(command.id()));
        quote.reject();
        return QuoteDto.from(repository.save(quote));
    }
}
