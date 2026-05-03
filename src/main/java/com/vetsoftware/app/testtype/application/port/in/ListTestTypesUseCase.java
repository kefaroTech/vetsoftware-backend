package com.vetsoftware.app.testtype.application.port.in;

import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListTestTypesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<TestTypeDto> listAll();
}
