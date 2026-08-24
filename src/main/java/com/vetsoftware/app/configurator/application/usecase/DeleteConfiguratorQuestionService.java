package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionHasActiveChildrenException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La baja es lógica y por eso hace falta el guardián: la FK
 * {@code ON DELETE RESTRICT} protege del borrado físico, que aquí no ocurre
 * nunca. Sin esta comprobación, dar de baja una pregunta dejaría sus opciones
 * vivas y sus efectos disparándose desde una rama que ya nadie muestra.
 */
@Observed(name = "configurator.question.delete")
@Service
public class DeleteConfiguratorQuestionService implements DeleteConfiguratorQuestionUseCase {

    private final ConfiguratorQuestionRepository repository;
    private final ConfiguratorOptionRepository optionRepository;
    private final ConfiguratorEffectRepository effectRepository;

    public DeleteConfiguratorQuestionService(ConfiguratorQuestionRepository repository,
            ConfiguratorOptionRepository optionRepository,
            ConfiguratorEffectRepository effectRepository) {
        this.repository = repository;
        this.optionRepository = optionRepository;
        this.effectRepository = effectRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConfiguratorQuestionNotFoundException(id));
        if (optionRepository.existsByQuestionId(id)) {
            throw new ConfiguratorQuestionHasActiveChildrenException("ConfiguratorQuestion", id,
                    "option");
        }
        if (effectRepository.existsByQuestionId(id)) {
            throw new ConfiguratorQuestionHasActiveChildrenException("ConfiguratorQuestion", id,
                    "effect");
        }
        repository.delete(id);
    }
}
