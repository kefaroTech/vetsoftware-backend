package com.vetsoftware.app.supplierwithholding.application.port.in;

import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSupplierWithholdingUseCase {

    /** Una retencion por su id. */
    @PreAuthorize("hasRole('SYSTEM')")
    SupplierWithholdingDto findById(Long id);
}
