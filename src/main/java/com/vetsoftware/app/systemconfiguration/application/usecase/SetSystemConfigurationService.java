package com.vetsoftware.app.systemconfiguration.application.usecase;

import com.vetsoftware.app.systemconfiguration.application.command.SetSystemConfigurationCommand;
import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import com.vetsoftware.app.systemconfiguration.application.port.in.SetSystemConfigurationUseCase;
import com.vetsoftware.app.systemconfiguration.application.port.out.SystemConfigurationRepository;
import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "systemConfiguration.set")
@Service
public class SetSystemConfigurationService implements SetSystemConfigurationUseCase {
    private final SystemConfigurationRepository repository;

    public SetSystemConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SystemConfigurationDto execute(SetSystemConfigurationCommand command) {
        SystemConfiguration config = repository.findByPropertyName(command.propertyName())
                .map(existing -> {
                    existing.update(command.value());
                    return existing;
                })
                .orElseGet(() -> SystemConfiguration.create(command.propertyName(), command.value()));
        return SystemConfigurationDto.from(repository.save(config));
    }
}
