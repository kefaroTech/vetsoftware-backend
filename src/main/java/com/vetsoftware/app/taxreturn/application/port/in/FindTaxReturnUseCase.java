package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindTaxReturnUseCase {

    /** Una declaracion por su id. */
    @PreAuthorize("hasRole('SYSTEM')")
    TaxReturnDto findById(Long id);
}
