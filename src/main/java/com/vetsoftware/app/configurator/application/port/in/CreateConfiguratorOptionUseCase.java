package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateConfiguratorOptionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorOptionDto execute(CreateConfiguratorOptionCommand command);
}
