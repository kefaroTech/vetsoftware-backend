package com.vetsoftware.app.configurator.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteConfiguratorQuestionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long id);
}
