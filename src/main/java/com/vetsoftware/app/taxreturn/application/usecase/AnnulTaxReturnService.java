package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.AnnulTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anula un borrador.
 *
 * <p>
 * <strong>Anular no es borrar.</strong> La fila se queda porque el numero de
 * secuencia ya esta gastado y {@code uq_tax_returns_case} lo recuerda: si se
 * borrara, la siguiente declaracion del mismo periodo reutilizaria el 1 y el
 * historico de correcciones dejaria de ser una serie.
 */
@Observed(name = "tax.return.annul")
@Service
public class AnnulTaxReturnService implements AnnulTaxReturnUseCase {

    private final TaxReturnRepository repository;

    public AnnulTaxReturnService(TaxReturnRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TaxReturnDto execute(Long id) {
        TaxReturn taxReturn = repository.findById(id)
                .orElseThrow(() -> new TaxReturnNotFoundException(id));
        return TaxReturnDto.from(repository.save(taxReturn.annul()));
    }
}
