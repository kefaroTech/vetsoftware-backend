package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorOptionsByQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "configurator.option.list")
@Service
public class ListConfiguratorOptionsByQuestionService
        implements
            ListConfiguratorOptionsByQuestionUseCase {

    private final ConfiguratorOptionRepository repository;

    public ListConfiguratorOptionsByQuestionService(ConfiguratorOptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConfiguratorOptionDto> listByQuestion(Long questionId) {
        return repository.findByQuestionId(questionId).stream().map(ConfiguratorOptionDto::from)
                .toList();
    }
}
