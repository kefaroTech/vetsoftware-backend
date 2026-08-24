package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.quote.application.port.in.ListQuotesUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** El embudo completo de la consola de plataforma. Solo SYSTEM lo alcanza. */
@Observed(name = "quote.list")
@Service
public class ListQuotesService implements ListQuotesUseCase {

    private final QuoteRepository repository;

    public ListQuotesService(QuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<QuoteSummaryDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(QuoteSummaryDto::from);
    }
}
