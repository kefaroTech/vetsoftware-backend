package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindConfiguratorQuestionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorQuestionDto findById(Long id);
}
