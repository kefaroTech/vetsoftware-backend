package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.in.FindConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "configurator.question.find")
@Service
public class FindConfiguratorQuestionService implements FindConfiguratorQuestionUseCase {

    private final ConfiguratorQuestionRepository repository;
    private final ConfiguratorOptionRepository optionRepository;

    public FindConfiguratorQuestionService(ConfiguratorQuestionRepository repository,
            ConfiguratorOptionRepository optionRepository) {
        this.repository = repository;
        this.optionRepository = optionRepository;
    }

    /**
     * Trae tambien las opciones para que la ficha y el listado hablen el mismo
     * contrato: si solo una de las dos las anida, el cliente escribe dos caminos
     * para pintar la misma pregunta.
     */
    @Override
    public ConfiguratorQuestionDto findById(Long id) {
        return repository.findById(id)
                .map(question -> ConfiguratorQuestionDto.from(question,
                        optionRepository.findByQuestionId(question.getId())))
                .orElseThrow(() -> new ConfiguratorQuestionNotFoundException(id));
    }
}
