package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorCodeAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El alta <strong>reactiva</strong> la pregunta retirada que ya ocupaba el
 * código en vez de insertar otra, que es el patrón que las tres tablas puente
 * de {@code catalogitem} sentaron antes.
 *
 * <p>
 * La guarda no puede ser {@code existsByCode}: esa consulta pasa por el
 * {@code @SQLRestriction} de la entidad y solo ve las filas activas, mientras
 * {@code uq_configurator_questions_code} no ignora nada. Con la guarda ciega,
 * volver a dar de alta un código retirado dice «libre», el {@code INSERT}
 * choca, y el administrador recibe un 409 genérico sobre una fila que no puede
 * ver: el código queda inutilizable desde la consola para siempre.
 */
@Observed(name = "configurator.question.create")
@Service
public class CreateConfiguratorQuestionService implements CreateConfiguratorQuestionUseCase {

    private final ConfiguratorQuestionRepository repository;
    private final ConfiguratorOptionRepository optionRepository;
    private final Clock clock;

    public CreateConfiguratorQuestionService(ConfiguratorQuestionRepository repository,
            ConfiguratorOptionRepository optionRepository, Clock clock) {
        this.repository = repository;
        this.optionRepository = optionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ConfiguratorQuestionDto execute(CreateConfiguratorQuestionCommand command) {
        // La entidad se construye primero porque valida las invariantes del comando:
        // un comando inválido no debe llegar a consultar nada.
        ConfiguratorQuestion question = ConfiguratorQuestion.create(command.code(),
                command.questionText(), command.helpText(), command.answerType(),
                command.parentOptionId(), command.required(), command.sortOrder(), clock);
        Optional<LinkStateDto> existente = repository.findAnyByCode(command.code());
        if (existente.isPresent()) {
            return revivir(existente.get(), command);
        }
        // Una pregunta nueva no puede cerrar un ciclo —nadie la apunta todavía— pero
        // sí puede colgar de una rama que ya lo tiene, y entonces nace inalcanzable.
        ConditionalQuestionGuard.assertParentIsUsable(null, command.parentOptionId(), repository,
                optionRepository);
        return ConfiguratorQuestionDto.from(repository.save(question));
    }

    /**
     * La reactivación va <strong>antes</strong> del guardián de ciclos y no
     * después: {@code ConditionalQuestionGuard} recorre el árbol con
     * {@code findAllOrdered()}, que solo ve las preguntas activas, así que con la
     * fila todavía apagada el recorrido no la encontraría y un ciclo cerrado por
     * ella pasaría inadvertido. Si el guardián rechaza, la transacción deshace la
     * reactivación.
     *
     * <p>
     * No hace falta revisar los efectos {@code QUANTITY_FROM_ANSWER} que colgaran
     * de la pregunta cuando se retiró: {@code DeleteConfiguratorQuestionService} no
     * deja darla de baja mientras tenga efectos activos, así que una pregunta
     * apagada no tiene ninguno que el nuevo {@code answerType} pueda romper.
     */
    private ConfiguratorQuestionDto revivir(LinkStateDto estado,
            CreateConfiguratorQuestionCommand command) {
        if (estado.enabled()) {
            throw new ConfiguratorCodeAlreadyExistsException("ConfiguratorQuestion",
                    command.code());
        }
        repository.reactivate(estado.id());
        ConditionalQuestionGuard.assertParentIsUsable(estado.id(), command.parentOptionId(),
                repository, optionRepository);
        ConfiguratorQuestion revivida = repository.findById(estado.id())
                .orElseThrow(() -> new ConfiguratorQuestionNotFoundException(estado.id()));
        revivida.update(command.questionText(), command.helpText(), command.answerType(),
                command.parentOptionId(), command.required(), command.sortOrder());
        return ConfiguratorQuestionDto.from(repository.save(revivida));
    }
}
