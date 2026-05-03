package com.vetsoftware.app.testtype.application.port.in;

import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    TestTypeDto findById(Long id);
}
