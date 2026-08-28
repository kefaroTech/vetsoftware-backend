package com.vetsoftware.app.uvtvalue.application.port.in;

import com.vetsoftware.app.uvtvalue.application.command.CreateUvtValueCommand;
import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Escribe la plataforma y solo la plataforma. */
public interface CreateUvtValueUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    UvtValueDto execute(CreateUvtValueCommand command);
}
