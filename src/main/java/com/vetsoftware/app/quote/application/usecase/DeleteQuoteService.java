package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.port.in.DeleteQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja logica de un borrador.
 *
 * <p>
 * Se carga primero -acotado cuando hay empresa- por dos motivos: para que un id
 * de otro tenant devuelva 404 en vez de ejecutar un UPDATE a ciegas, y para
 * comprobar que sigue siendo DRAFT. Una oferta ya enviada no se puede esconder:
 * desactivarla borraria la prueba de lo que se le ofrecio al cliente, y ademas
 * descuadraria el total sin borrar nada -que es justo lo que caza la consulta
 * de vigilancia de R5-.
 */
@Observed(name = "quote.delete")
@Service
public class DeleteQuoteService implements DeleteQuoteUseCase {

    private final QuoteRepository repository;

    public DeleteQuoteService(QuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        Quote quote = (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new QuoteNotFoundException(id));
        quote.requireDeletable();
        // Una cotizacion a prospecto tiene company_id NULL: ningun
        // WHERE company_id = ? casaria con ella, asi que su unico camino de baja
        // es la sobrecarga ancha, ya restringida a SYSTEM por el @PreAuthorize.
        if (companyId == null) {
            repository.softDelete(id);
        } else {
            repository.softDelete(id, companyId);
        }
    }
}
