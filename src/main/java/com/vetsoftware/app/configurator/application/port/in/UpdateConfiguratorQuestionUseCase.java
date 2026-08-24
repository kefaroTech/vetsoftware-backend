package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateConfiguratorQuestionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorQuestionDto execute(UpdateConfiguratorQuestionCommand command);
}
