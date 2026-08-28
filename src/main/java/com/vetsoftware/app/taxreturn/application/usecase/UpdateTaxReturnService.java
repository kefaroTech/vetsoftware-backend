package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.taxreturn.application.command.UpdateTaxReturnAmountsCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.UpdateTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrige los importes de un borrador.
 *
 * <p>
 * Que la declaracion siga siendo borrador lo decide el dominio, no este metodo:
 * {@code chk_tax_returns_filed} mira la fila y no de donde venia, asi que
 * reeditar una presentada produce una fila que el motor acepta —y unos numeros
 * que ya no coinciden con el formulario radicado ante la DIAN.
 *
 * <p>
 * Leer, modificar y guardar dentro de una transaccion, con {@code @Version} de
 * por medio: dos ediciones concurrentes no se pisan.
 */
@Observed(name = "tax.return.update")
@Service
public class UpdateTaxReturnService implements UpdateTaxReturnUseCase {

    private final TaxReturnRepository repository;

    public UpdateTaxReturnService(TaxReturnRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TaxReturnDto execute(UpdateTaxReturnAmountsCommand command) {
        TaxReturn taxReturn = repository.findById(command.id())
                .orElseThrow(() -> new TaxReturnNotFoundException(command.id()));
        return TaxReturnDto.from(repository.save(taxReturn.updateAmounts(command.totalGenerated(),
                command.totalDeductible(), command.balancePayable(), command.balanceCredit())));
    }
}
