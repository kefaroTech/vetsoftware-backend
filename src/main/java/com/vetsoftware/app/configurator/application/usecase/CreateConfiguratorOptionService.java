package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorCodeAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mismo patrón que {@link CreateConfiguratorQuestionService}: la opción
 * retirada que ya ocupaba {@code (question_id, code)} se
 * <strong>reactiva</strong> en vez de insertar otra. El escenario que lo hace
 * necesario es corriente: un administrador retira la opción {@code YES} de una
 * pregunta y una semana después la vuelve a necesitar.
 */
@Observed(name = "configurator.option.create")
@Service
public class CreateConfiguratorOptionService implements CreateConfiguratorOptionUseCase {

    private final ConfiguratorOptionRepository repository;
    private final ConfiguratorQuestionRepository questionRepository;
    private final Clock clock;

    public CreateConfiguratorOptionService(ConfiguratorOptionRepository repository,
            ConfiguratorQuestionRepository questionRepository, Clock clock) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ConfiguratorOptionDto execute(CreateConfiguratorOptionCommand command) {
        ConfiguratorQuestion pregunta = questionRepository.findById(command.questionId())
                .orElseThrow(() -> new ConfiguratorQuestionNotFoundException(command.questionId()));
        // La fila ya está cargada para el 404, así que la coherencia de tipo no
        // cuesta ni una consulta más.
        NumberQuestionGuard.assertQuestionAdmitsOptions(pregunta);
        // El codigo es unico POR PREGUNTA: dos preguntas pueden tener su propia
        // opcion YES, y una unicidad global lo impediria. La consulta ignora el
        // borrado logico porque uq_configurator_options_code tampoco lo mira.
        ConfiguratorOption option = ConfiguratorOption.create(command.questionId(), command.code(),
                command.label(), command.helpText(), command.sortOrder(), clock);
        Optional<LinkStateDto> existente = repository
                .findAnyByQuestionIdAndCode(command.questionId(), command.code());
        if (existente.isPresent()) {
            LinkStateDto estado = existente.get();
            if (estado.enabled()) {
                throw new ConfiguratorCodeAlreadyExistsException("ConfiguratorOption",
                        command.code());
            }
            repository.reactivate(estado.id());
            ConfiguratorOption revivida = repository.findById(estado.id())
                    .orElseThrow(() -> new ConfiguratorOptionNotFoundException(estado.id()));
            revivida.update(command.label(), command.helpText(), command.sortOrder());
            return ConfiguratorOptionDto.from(repository.save(revivida));
        }
        return ConfiguratorOptionDto.from(repository.save(option));
    }
}
