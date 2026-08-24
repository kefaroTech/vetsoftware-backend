package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateConfiguratorEffectUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorEffectDto execute(CreateConfiguratorEffectCommand command);
}
