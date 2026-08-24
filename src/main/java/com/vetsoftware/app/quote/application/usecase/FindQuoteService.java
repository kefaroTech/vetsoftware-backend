package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.FindQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Lee la cotizacion.
 *
 * <p>
 * El ternario NO es un atajo: con companyId la carga va acotada, y sin el -solo
 * alcanzable por SYSTEM, y unico camino para una oferta a prospecto, cuyo
 * company_id es NULL- va ancha. Cargar siempre ancho y comprobar despues en
 * Java es la fuga que CARGA_POR_ID_ACOTADA_POR_EMPRESA existe para cerrar.
 */
@Observed(name = "quote.find")
@Service
public class FindQuoteService implements FindQuoteUseCase {

    private final QuoteRepository repository;

    public FindQuoteService(QuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public QuoteDto findById(Long id, Long companyId) {
        Quote quote = (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new QuoteNotFoundException(id));
        return QuoteDto.from(quote);
    }
}
