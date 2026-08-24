package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.configurator.application.port.in.ResolveConfiguratorSelectionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorAnswerCoherence;
import com.vetsoftware.app.configurator.domain.ConfiguratorAnswers;
import com.vetsoftware.app.configurator.domain.ConfiguratorResolver;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tres consultas y dos funciones puras: primero se comprueba que las respuestas
 * encajen en el árbol, y solo entonces se traducen en carrito.
 *
 * <p>
 * <strong>El orden importa y es el punto del caso de uso.</strong> Resolver
 * primero y validar después no serviría de nada: para cuando la selección está
 * hecha, el artículo colado ya está dentro. La comprobación de coherencia es
 * una precondición, no una revisión.
 *
 * <p>
 * Las dos consultas extra —preguntas y opciones, además de los efectos— son el
 * precio de que {@code parent_option_id} signifique algo al resolver. Sobre
 * decenas de filas es irrelevante, y la alternativa era un cuestionario
 * condicional decorativo: sus condiciones se comprobaban al guardarlo y nunca
 * al usarlo.
 *
 * <p>
 * Todo el criterio vive en el dominio —{@link ConfiguratorAnswerCoherence} y
 * {@link ConfiguratorResolver}—; este servicio solo trae los datos y traduce el
 * command. Esa separación es lo que permite probar el carrito, que es donde un
 * error se convierte en una cotización equivocada, sin base de datos ni
 * contexto de Spring.
 */
@Observed(name = "configurator.selection.resolve")
@Service
public class ResolveConfiguratorSelectionService implements ResolveConfiguratorSelectionUseCase {

    private final ConfiguratorEffectRepository repository;
    private final ConfiguratorQuestionRepository questionRepository;
    private final ConfiguratorOptionRepository optionRepository;

    public ResolveConfiguratorSelectionService(ConfiguratorEffectRepository repository,
            ConfiguratorQuestionRepository questionRepository,
            ConfiguratorOptionRepository optionRepository) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguratorSelectionDto resolve(ResolveConfiguratorSelectionCommand command) {
        ConfiguratorAnswers answers = new ConfiguratorAnswers(command.selectedOptionIds(),
                command.numericAnswers());
        ConfiguratorAnswerCoherence.assertCoherent(questionRepository.findAllOrdered(),
                optionRepository.findAllOrdered(), answers);
        return ConfiguratorSelectionDto
                .from(ConfiguratorResolver.resolve(repository.findAllOrdered(), answers));
    }
}
