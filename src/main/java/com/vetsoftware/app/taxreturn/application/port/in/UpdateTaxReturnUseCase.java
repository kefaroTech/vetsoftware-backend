package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.taxreturn.application.command.UpdateTaxReturnAmountsCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateTaxReturnUseCase {

    /**
     * Corrige los importes de un borrador. <strong>Solo en {@code DRAFT}</strong>:
     * una declaracion presentada no se edita, se sucede — y para eso esta
     * {@code CorrectTaxReturnUseCase}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    TaxReturnDto execute(UpdateTaxReturnAmountsCommand command);
}
