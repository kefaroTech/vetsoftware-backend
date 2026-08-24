package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.dto.QuestionnaireOptionDto;
import com.vetsoftware.app.configurator.application.dto.QuestionnaireQuestionDto;
import com.vetsoftware.app.configurator.application.port.in.GetPublicQuestionnaireUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El cuestionario entero en <strong>dos consultas</strong>, no en una por
 * pregunta.
 *
 * <p>
 * Se traen todas las preguntas y todas las opciones y se agrupan en memoria.
 * Sobre decenas de filas eso es más barato que cualquier {@code JOIN}, y sobre
 * todo es lo que evita el N+1 que produciría recorrer las preguntas pidiendo
 * sus opciones — en el endpoint que sirve a gente sin autenticar, que es
 * exactamente donde un N+1 se convierte en una vía de saturación gratuita.
 */
@Observed(name = "configurator.questionnaire.get")
@Service
public class GetPublicQuestionnaireService implements GetPublicQuestionnaireUseCase {

    private final ConfiguratorQuestionRepository questionRepository;
    private final ConfiguratorOptionRepository optionRepository;

    public GetPublicQuestionnaireService(ConfiguratorQuestionRepository questionRepository,
            ConfiguratorOptionRepository optionRepository) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireQuestionDto> get() {
        Map<Long, List<ConfiguratorOption>> porPregunta = optionRepository.findAllOrdered().stream()
                .collect(Collectors.groupingBy(ConfiguratorOption::getQuestionId));

        return questionRepository.findAllOrdered().stream().map(question -> QuestionnaireQuestionDto
                .from(question, opcionesDe(porPregunta, question.getId()))).toList();
    }

    private static List<QuestionnaireOptionDto> opcionesDe(
            Map<Long, List<ConfiguratorOption>> porPregunta, Long questionId) {
        return porPregunta.getOrDefault(questionId, List.of()).stream()
                .map(QuestionnaireOptionDto::from).toList();
    }
}
