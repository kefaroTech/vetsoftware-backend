package com.vetsoftware.app.testtype.application.port.in;

import com.vetsoftware.app.testtype.application.command.UpdateTestTypeCommand;
import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    TestTypeDto execute(UpdateTestTypeCommand command);
}
