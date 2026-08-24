package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El disparador no se edita —vive en campos finales de la entidad— pero el
 * <em>tipo de efecto</em> sí, y ahí está la trampa: pasar un {@code ADD} a
 * {@code QUANTITY_FROM_ANSWER} sobre un efecto colgado de una opción es
 * exactamente la incoherencia que se rechaza al crear, entrando por la puerta
 * de atrás. Por eso la comprobación se repite aquí contra el disparador ya
 * guardado.
 */
@Observed(name = "configurator.effect.update")
@Service
public class UpdateConfiguratorEffectService implements UpdateConfiguratorEffectUseCase {

    private final ConfiguratorEffectRepository repository;
    private final ConfiguratorQuestionRepository questionRepository;
    private final ConfiguratorOptionRepository optionRepository;
    private final CatalogItemValidationPort catalogItemValidationPort;

    public UpdateConfiguratorEffectService(ConfiguratorEffectRepository repository,
            ConfiguratorQuestionRepository questionRepository,
            ConfiguratorOptionRepository optionRepository,
            CatalogItemValidationPort catalogItemValidationPort) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.catalogItemValidationPort = catalogItemValidationPort;
    }

    @Override
    @Transactional
    public ConfiguratorEffectDto execute(UpdateConfiguratorEffectCommand command) {
        ConfiguratorEffect effect = repository.findById(command.id())
                .orElseThrow(() -> new ConfiguratorEffectNotFoundException(command.id()));
        if (!catalogItemValidationPort.existsById(command.catalogItemId())) {
            throw new IllegalArgumentException(
                    "Catalog item not found: " + command.catalogItemId());
        }
        QuantityFromAnswerGuard.assertCoherent(command.effect(), effect.getOptionId(),
                effect.getQuestionId(), questionRepository, optionRepository);
        effect.update(command.catalogItemId(), command.effect(), command.quantity());
        return ConfiguratorEffectDto.from(repository.save(effect));
    }
}
