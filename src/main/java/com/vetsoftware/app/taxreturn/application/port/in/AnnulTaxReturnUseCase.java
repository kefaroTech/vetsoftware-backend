package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnnulTaxReturnUseCase {

    /**
     * Anula un borrador. Es lo que sustituye al borrado: la fila se queda —el
     * numero de secuencia ya esta gastado y {@code uq_tax_returns_case} lo
     * recuerda— y lo que cambia es que deja de ocupar el hueco de
     * {@code uq_tax_returns_current}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    TaxReturnDto execute(Long id);
}
