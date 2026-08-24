package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Es el camino por el que un ciclo entra de verdad. Crear una pregunta
 * condicional apuntando hacia arriba es difícil de hacer mal; reapuntar una que
 * ya tiene descendencia, no.
 *
 * <p>
 * Y es también el camino por el que se rompe en silencio la otra invariante del
 * slice: cambiar el {@code answerType} de {@code NUMBER} a cualquier otra cosa
 * deja vivos los efectos {@code QUANTITY_FROM_ANSWER} que leían ese número. Por
 * eso hay dos guardianes y no uno.
 *
 * <p>
 * Y el mismo campo rompe una tercera en la dirección contraria: pasar
 * <em>a</em> {@code NUMBER} una pregunta que ya tiene opciones las deja vivas
 * colgando de una pregunta que se responde escribiendo un número. De ahí el
 * tercero.
 */
@Observed(name = "configurator.question.update")
@Service
public class UpdateConfiguratorQuestionService implements UpdateConfiguratorQuestionUseCase {

    private final ConfiguratorQuestionRepository repository;
    private final ConfiguratorOptionRepository optionRepository;
    private final ConfiguratorEffectRepository effectRepository;

    public UpdateConfiguratorQuestionService(ConfiguratorQuestionRepository repository,
            ConfiguratorOptionRepository optionRepository,
            ConfiguratorEffectRepository effectRepository) {
        this.repository = repository;
        this.optionRepository = optionRepository;
        this.effectRepository = effectRepository;
    }

    @Override
    @Transactional
    public ConfiguratorQuestionDto execute(UpdateConfiguratorQuestionCommand command) {
        ConfiguratorQuestion question = repository.findById(command.id())
                .orElseThrow(() -> new ConfiguratorQuestionNotFoundException(command.id()));
        ConditionalQuestionGuard.assertParentIsUsable(command.id(), command.parentOptionId(),
                repository, optionRepository);
        QuantityFromAnswerGuard.assertQuestionTypeStillFits(command.id(), command.answerType(),
                effectRepository);
        NumberQuestionGuard.assertNoOptionsInTheWay(command.id(), question.getCode(),
                command.answerType(), optionRepository);
        question.update(command.questionText(), command.helpText(), command.answerType(),
                command.parentOptionId(), command.required(), command.sortOrder());
        return ConfiguratorQuestionDto.from(repository.save(question));
    }
}
