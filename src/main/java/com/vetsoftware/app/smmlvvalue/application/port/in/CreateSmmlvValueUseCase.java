package com.vetsoftware.app.smmlvvalue.application.port.in;

import com.vetsoftware.app.smmlvvalue.application.command.CreateSmmlvValueCommand;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSmmlvValueUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SmmlvValueDto execute(CreateSmmlvValueCommand command);
}
