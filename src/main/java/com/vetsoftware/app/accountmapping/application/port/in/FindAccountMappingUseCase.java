package com.vetsoftware.app.accountmapping.application.port.in;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindAccountMappingUseCase {

    /** Un mapeo por su id. */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountMappingDto findById(Long id);
}
