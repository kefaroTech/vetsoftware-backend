package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.dto.QuoteTotalsMismatchDto;
import com.vetsoftware.app.quote.application.port.in.FindQuoteTotalsMismatchesUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "quote.totals.watch")
@Service
public class FindQuoteTotalsMismatchesService implements FindQuoteTotalsMismatchesUseCase {

    private final QuoteRepository repository;

    public FindQuoteTotalsMismatchesService(QuoteRepository repository) {
        this.repository = repository;
    }

    /**
     * Solo lectura y sin ninguna decision: lo que devuelve la consulta, tal cual.
     */
    @Override
    @Transactional(readOnly = true)
    public List<QuoteTotalsMismatchDto> findAllTotalsMismatches() {
        return repository.findAllTotalsMismatches();
    }
}
