package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateConfiguratorEffectUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorEffectDto execute(UpdateConfiguratorEffectCommand command);
}
