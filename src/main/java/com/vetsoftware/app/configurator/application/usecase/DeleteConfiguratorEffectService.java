package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "configurator.effect.delete")
@Service
public class DeleteConfiguratorEffectService implements DeleteConfiguratorEffectUseCase {

    private final ConfiguratorEffectRepository repository;

    public DeleteConfiguratorEffectService(ConfiguratorEffectRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConfiguratorEffectNotFoundException(id));
        repository.delete(id);
    }
}
