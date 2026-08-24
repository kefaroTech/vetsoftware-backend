package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorQuestionsUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Observed(name = "configurator.question.list")
@Service
public class ListConfiguratorQuestionsService implements ListConfiguratorQuestionsUseCase {

    private final ConfiguratorQuestionRepository repository;
    private final ConfiguratorOptionRepository optionRepository;

    public ListConfiguratorQuestionsService(ConfiguratorQuestionRepository repository,
            ConfiguratorOptionRepository optionRepository) {
        this.repository = repository;
        this.optionRepository = optionRepository;
    }

    /**
     * Dos consultas por pagina y no {@code 1 + N}: las opciones de todas las
     * preguntas de la pagina se resuelven de golpe (incidencia #448).
     */
    @Override
    public PageResult<ConfiguratorQuestionDto> listAll(int page, int pageSize) {
        PageResult<ConfiguratorQuestion> pagina = repository.findAll(page, pageSize);
        Map<Long, List<ConfiguratorOption>> opciones = optionRepository.findByQuestionIds(
                pagina.content().stream().map(ConfiguratorQuestion::getId).toList());
        return pagina.map(question -> ConfiguratorQuestionDto.from(question,
                opciones.getOrDefault(question.getId(), List.of())));
    }
}
