package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aquí se paga por adelantado la coherencia del carrito. Las comprobaciones que
 * hace este servicio —el artículo existe, el disparador existe,
 * {@code QUANTITY_FROM_ANSWER} cuelga de una pregunta numérica y la terna no
 * está ya ocupada— no las puede hacer la base con un mensaje útil, y la
 * alternativa a hacerlas al guardar es descubrirlas cotizando.
 *
 * <p>
 * La última es la que cierra la trampa del borrado lógico: las dos claves
 * únicas del efecto no incluyen {@code enabled}, así que un efecto retirado
 * sigue ocupando su terna siendo invisible para la aplicación. Como en las
 * tablas puente de {@code catalogitem}, el alta <strong>reactiva</strong> esa
 * fila en vez de insertar otra.
 */
@Observed(name = "configurator.effect.create")
@Service
public class CreateConfiguratorEffectService implements CreateConfiguratorEffectUseCase {

    private final ConfiguratorEffectRepository repository;
    private final ConfiguratorQuestionRepository questionRepository;
    private final ConfiguratorOptionRepository optionRepository;
    private final CatalogItemValidationPort catalogItemValidationPort;
    private final Clock clock;

    public CreateConfiguratorEffectService(ConfiguratorEffectRepository repository,
            ConfiguratorQuestionRepository questionRepository,
            ConfiguratorOptionRepository optionRepository,
            CatalogItemValidationPort catalogItemValidationPort, Clock clock) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.catalogItemValidationPort = catalogItemValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ConfiguratorEffectDto execute(CreateConfiguratorEffectCommand command) {
        if (!catalogItemValidationPort.existsById(command.catalogItemId())) {
            throw new IllegalArgumentException(
                    "Catalog item not found: " + command.catalogItemId());
        }
        assertTriggerExists(command.optionId(), command.questionId());
        QuantityFromAnswerGuard.assertCoherent(command.effect(), command.optionId(),
                command.questionId(), questionRepository, optionRepository);

        // La entidad se construye antes de consultar la terna porque es quien exige
        // «exactamente un disparador» y la coherencia de quantity: un comando que
        // viola esas invariantes no debe llegar a tocar el repositorio.
        ConfiguratorEffect effect = ConfiguratorEffect.create(command.optionId(),
                command.questionId(), command.catalogItemId(), command.effect(), command.quantity(),
                clock);
        Optional<LinkStateDto> existente = repository.findAnyByTrigger(command.optionId(),
                command.questionId(), command.catalogItemId(), command.effect());
        if (existente.isPresent()) {
            return revivir(existente.get(), command);
        }
        return ConfiguratorEffectDto.from(repository.save(effect));
    }

    /**
     * El disparador, el artículo y el tipo de efecto son los tres inmutables de la
     * fila revivida y coinciden con los del comando —es la terna por la que se
     * encontró—, así que lo único que el alta puede aportar es la cantidad.
     */
    private ConfiguratorEffectDto revivir(LinkStateDto estado,
            CreateConfiguratorEffectCommand command) {
        if (estado.enabled()) {
            throw new ConfiguratorEffectAlreadyExistsException(command.optionId(),
                    command.questionId(), command.catalogItemId(), command.effect());
        }
        repository.reactivate(estado.id());
        ConfiguratorEffect revivido = repository.findById(estado.id())
                .orElseThrow(() -> new ConfiguratorEffectNotFoundException(estado.id()));
        revivido.update(command.catalogItemId(), command.effect(), command.quantity());
        return ConfiguratorEffectDto.from(repository.save(revivido));
    }

    /**
     * Que venga exactamente uno de los dos lo comprueba la entidad; que la fila
     * exista, este servicio. Son dos cosas distintas y la base solo garantiza la
     * segunda al insertar, con un error que no dice cuál de los dos ids falla.
     */
    private void assertTriggerExists(Long optionId, Long questionId) {
        if (optionId != null) {
            optionRepository.findById(optionId)
                    .orElseThrow(() -> new ConfiguratorOptionNotFoundException(optionId));
            return;
        }
        if (questionId != null) {
            questionRepository.findById(questionId)
                    .orElseThrow(() -> new ConfiguratorQuestionNotFoundException(questionId));
        }
    }
}
