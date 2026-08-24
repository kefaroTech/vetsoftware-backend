package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionHasActiveChildrenException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * De una opción cuelgan dos cosas distintas y las dos hay que mirar: los
 * efectos que dispara y las preguntas condicionales que abre. Olvidar la
 * segunda es lo que deja una rama entera del cuestionario colgando de una
 * opción invisible.
 */
@Observed(name = "configurator.option.delete")
@Service
public class DeleteConfiguratorOptionService implements DeleteConfiguratorOptionUseCase {

    private final ConfiguratorOptionRepository repository;
    private final ConfiguratorQuestionRepository questionRepository;
    private final ConfiguratorEffectRepository effectRepository;

    public DeleteConfiguratorOptionService(ConfiguratorOptionRepository repository,
            ConfiguratorQuestionRepository questionRepository,
            ConfiguratorEffectRepository effectRepository) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.effectRepository = effectRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConfiguratorOptionNotFoundException(id));
        if (effectRepository.existsByOptionId(id)) {
            throw new ConfiguratorQuestionHasActiveChildrenException("ConfiguratorOption", id,
                    "effect");
        }
        if (questionRepository.existsByParentOptionId(id)) {
            throw new ConfiguratorQuestionHasActiveChildrenException("ConfiguratorOption", id,
                    "conditional question");
        }
        repository.delete(id);
    }
}
