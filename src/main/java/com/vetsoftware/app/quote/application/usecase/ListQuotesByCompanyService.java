package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.quote.application.port.in.ListQuotesByCompanyUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "quote.list.by.company")
@Service
public class ListQuotesByCompanyService implements ListQuotesByCompanyUseCase {

    private final QuoteRepository repository;

    public ListQuotesByCompanyService(QuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<QuoteSummaryDto> listByCompany(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize).map(QuoteSummaryDto::from);
    }
}
