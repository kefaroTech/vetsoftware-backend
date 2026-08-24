package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateConfiguratorOptionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorOptionDto execute(UpdateConfiguratorOptionCommand command);
}
