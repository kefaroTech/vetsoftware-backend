package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "configurator.option.update")
@Service
public class UpdateConfiguratorOptionService implements UpdateConfiguratorOptionUseCase {

    private final ConfiguratorOptionRepository repository;

    public UpdateConfiguratorOptionService(ConfiguratorOptionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ConfiguratorOptionDto execute(UpdateConfiguratorOptionCommand command) {
        ConfiguratorOption option = repository.findById(command.id())
                .orElseThrow(() -> new ConfiguratorOptionNotFoundException(command.id()));
        option.update(command.label(), command.helpText(), command.sortOrder());
        return ConfiguratorOptionDto.from(repository.save(option));
    }
}
