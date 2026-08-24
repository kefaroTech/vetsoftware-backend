package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorEffectsUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "configurator.effect.list")
@Service
public class ListConfiguratorEffectsService implements ListConfiguratorEffectsUseCase {

    private final ConfiguratorEffectRepository repository;

    public ListConfiguratorEffectsService(ConfiguratorEffectRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ConfiguratorEffectDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(ConfiguratorEffectDto::from);
    }
}
