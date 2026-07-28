package com.vetsoftware.app.systemconfiguration.application.usecase;

import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import com.vetsoftware.app.systemconfiguration.application.port.in.ListSystemConfigurationsUseCase;
import com.vetsoftware.app.systemconfiguration.application.port.out.SystemConfigurationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "system.configuration.list")
@Service
public class ListSystemConfigurationsService implements ListSystemConfigurationsUseCase {
    private final SystemConfigurationRepository repository;

    public ListSystemConfigurationsService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SystemConfigurationDto> listAll() {
        return repository.findAll().stream().map(SystemConfigurationDto::from).toList();
    }
}
