package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.port.in.ExpireOverdueQuotesUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marca EXPIRED lo que ya vencio.
 *
 * <p>
 * Se hace cargando y guardando entidad a entidad, NO con un UPDATE masivo, y es
 * deliberado: quotes va versionada, asi que un UPDATE de conjunto tendria que
 * mover version en el SET para no ser pisado por un save concurrente (#53), y
 * ademas tendria que nombrar la empresa para
 * MUTACIONES_SQL_ACOTADAS_POR_EMPRESA, cosa que un barrido de plataforma no
 * puede hacer por definicion. Por el camino de la entidad gestionada, Hibernate
 * comprueba e incrementa la version el solo y el conflicto sale como 409 en vez
 * de como una escritura perdida en silencio. El volumen lo permite: son decenas
 * de filas al dia, no millones.
 */
@Observed(name = "quote.expire.overdue")
@Service
public class ExpireOverdueQuotesService implements ExpireOverdueQuotesUseCase {

    private final QuoteRepository repository;
    private final Clock clock;

    public ExpireOverdueQuotesService(QuoteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int expireOverdue(int batchSize) {
        LocalDate today = LocalDate.now(clock);
        List<Quote> quotes = repository.findExpirable(today, batchSize);
        for (Quote quote : quotes) {
            quote.expire(today);
            repository.save(quote);
        }
        return quotes.size();
    }
}
