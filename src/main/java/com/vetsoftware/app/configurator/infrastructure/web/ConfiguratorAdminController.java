package com.vetsoftware.app.configurator.infrastructure.web;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.command.CreateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.command.CreateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.FindConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorEffectsUseCase;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorOptionsByQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorQuestionsUseCase;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.infrastructure.web.request.CreateConfiguratorEffectRequest;
import com.vetsoftware.app.configurator.infrastructure.web.request.CreateConfiguratorOptionRequest;
import com.vetsoftware.app.configurator.infrastructure.web.request.CreateConfiguratorQuestionRequest;
import com.vetsoftware.app.configurator.infrastructure.web.request.UpdateConfiguratorEffectRequest;
import com.vetsoftware.app.configurator.infrastructure.web.request.UpdateConfiguratorOptionRequest;
import com.vetsoftware.app.configurator.infrastructure.web.request.UpdateConfiguratorQuestionRequest;
import com.vetsoftware.app.configurator.infrastructure.web.response.ConfiguratorEffectResponse;
import com.vetsoftware.app.configurator.infrastructure.web.response.ConfiguratorOptionResponse;
import com.vetsoftware.app.configurator.infrastructure.web.response.ConfiguratorQuestionResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola del cuestionario. Todos sus endpoints cuelgan de puertos
 * {@code hasRole('SYSTEM')}: no hay ninguna empresa dueña de una pregunta, y
 * sus tres tablas no tienen {@code company_id}.
 *
 * <p>
 * Las rutas viven bajo {@code /configurator} igual que las públicas, pero con
 * segmentos distintos ({@code /questions}, {@code /options}, {@code /effects})
 * y ninguna colisiona con {@code /questionnaire}. Ese detalle importa: lo que
 * abre la puerta pública es un patrón exacto, no un prefijo.
 */
@RestController
@RequestMapping("/configurator")
public class ConfiguratorAdminController {

    private final CreateConfiguratorQuestionUseCase createQuestionUseCase;
    private final UpdateConfiguratorQuestionUseCase updateQuestionUseCase;
    private final DeleteConfiguratorQuestionUseCase deleteQuestionUseCase;
    private final FindConfiguratorQuestionUseCase findQuestionUseCase;
    private final ListConfiguratorQuestionsUseCase listQuestionsUseCase;
    private final CreateConfiguratorOptionUseCase createOptionUseCase;
    private final UpdateConfiguratorOptionUseCase updateOptionUseCase;
    private final DeleteConfiguratorOptionUseCase deleteOptionUseCase;
    private final ListConfiguratorOptionsByQuestionUseCase listOptionsUseCase;
    private final CreateConfiguratorEffectUseCase createEffectUseCase;
    private final UpdateConfiguratorEffectUseCase updateEffectUseCase;
    private final DeleteConfiguratorEffectUseCase deleteEffectUseCase;
    private final ListConfiguratorEffectsUseCase listEffectsUseCase;

    public ConfiguratorAdminController(CreateConfiguratorQuestionUseCase createQuestionUseCase,
            UpdateConfiguratorQuestionUseCase updateQuestionUseCase,
            DeleteConfiguratorQuestionUseCase deleteQuestionUseCase,
            FindConfiguratorQuestionUseCase findQuestionUseCase,
            ListConfiguratorQuestionsUseCase listQuestionsUseCase,
            CreateConfiguratorOptionUseCase createOptionUseCase,
            UpdateConfiguratorOptionUseCase updateOptionUseCase,
            DeleteConfiguratorOptionUseCase deleteOptionUseCase,
            ListConfiguratorOptionsByQuestionUseCase listOptionsUseCase,
            CreateConfiguratorEffectUseCase createEffectUseCase,
            UpdateConfiguratorEffectUseCase updateEffectUseCase,
            DeleteConfiguratorEffectUseCase deleteEffectUseCase,
            ListConfiguratorEffectsUseCase listEffectsUseCase) {
        this.createQuestionUseCase = createQuestionUseCase;
        this.updateQuestionUseCase = updateQuestionUseCase;
        this.deleteQuestionUseCase = deleteQuestionUseCase;
        this.findQuestionUseCase = findQuestionUseCase;
        this.listQuestionsUseCase = listQuestionsUseCase;
        this.createOptionUseCase = createOptionUseCase;
        this.updateOptionUseCase = updateOptionUseCase;
        this.deleteOptionUseCase = deleteOptionUseCase;
        this.listOptionsUseCase = listOptionsUseCase;
        this.createEffectUseCase = createEffectUseCase;
        this.updateEffectUseCase = updateEffectUseCase;
        this.deleteEffectUseCase = deleteEffectUseCase;
        this.listEffectsUseCase = listEffectsUseCase;
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguratorQuestionResponse createQuestion(
            @Valid @RequestBody CreateConfiguratorQuestionRequest request) {
        return toResponse(createQuestionUseCase.execute(new CreateConfiguratorQuestionCommand(
                request.code(), request.questionText(), request.helpText(), request.answerType(),
                request.parentOptionId(), request.required(), request.sortOrder())));
    }

    @GetMapping("/questions")
    public PageResponse<ConfiguratorQuestionResponse> listQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listQuestionsUseCase.listAll(page, pageSize),
                ConfiguratorAdminController::toResponse);
    }

    @GetMapping("/questions/{id}")
    public ConfiguratorQuestionResponse findQuestion(@PathVariable Long id) {
        return toResponse(findQuestionUseCase.findById(id));
    }

    @PutMapping("/questions/{id}")
    public ConfiguratorQuestionResponse updateQuestion(@PathVariable Long id,
            @Valid @RequestBody UpdateConfiguratorQuestionRequest request) {
        return toResponse(updateQuestionUseCase.execute(new UpdateConfiguratorQuestionCommand(id,
                request.questionText(), request.helpText(), request.answerType(),
                request.parentOptionId(), request.required(), request.sortOrder())));
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long id) {
        deleteQuestionUseCase.execute(id);
    }

    @GetMapping("/questions/{questionId}/options")
    public List<ConfiguratorOptionResponse> listOptions(@PathVariable Long questionId) {
        return listOptionsUseCase.listByQuestion(questionId).stream()
                .map(ConfiguratorAdminController::toResponse).toList();
    }

    @PostMapping("/options")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguratorOptionResponse createOption(
            @Valid @RequestBody CreateConfiguratorOptionRequest request) {
        return toResponse(createOptionUseCase
                .execute(new CreateConfiguratorOptionCommand(request.questionId(), request.code(),
                        request.label(), request.helpText(), request.sortOrder())));
    }

    @PutMapping("/options/{id}")
    public ConfiguratorOptionResponse updateOption(@PathVariable Long id,
            @Valid @RequestBody UpdateConfiguratorOptionRequest request) {
        return toResponse(updateOptionUseCase.execute(new UpdateConfiguratorOptionCommand(id,
                request.label(), request.helpText(), request.sortOrder())));
    }

    @DeleteMapping("/options/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOption(@PathVariable Long id) {
        deleteOptionUseCase.execute(id);
    }

    @PostMapping("/effects")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguratorEffectResponse createEffect(
            @Valid @RequestBody CreateConfiguratorEffectRequest request) {
        return toResponse(createEffectUseCase.execute(
                new CreateConfiguratorEffectCommand(request.optionId(), request.questionId(),
                        request.catalogItemId(), request.effect(), request.quantity())));
    }

    @GetMapping("/effects")
    public PageResponse<ConfiguratorEffectResponse> listEffects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listEffectsUseCase.listAll(page, pageSize),
                ConfiguratorAdminController::toResponse);
    }

    @PutMapping("/effects/{id}")
    public ConfiguratorEffectResponse updateEffect(@PathVariable Long id,
            @Valid @RequestBody UpdateConfiguratorEffectRequest request) {
        return toResponse(updateEffectUseCase.execute(new UpdateConfiguratorEffectCommand(id,
                request.catalogItemId(), request.effect(), request.quantity())));
    }

    @DeleteMapping("/effects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEffect(@PathVariable Long id) {
        deleteEffectUseCase.execute(id);
    }

    private static ConfiguratorQuestionResponse toResponse(ConfiguratorQuestionDto dto) {
        return new ConfiguratorQuestionResponse(dto.id(), dto.code(), dto.questionText(),
                dto.helpText(), dto.answerType().name(), dto.parentOptionId(), dto.required(),
                dto.sortOrder(), dto.createdDate(), dto.enabled(),
                dto.options().stream().map(ConfiguratorAdminController::toResponse).toList());
    }

    private static ConfiguratorOptionResponse toResponse(ConfiguratorOptionDto dto) {
        return new ConfiguratorOptionResponse(dto.id(), dto.questionId(), dto.code(), dto.label(),
                dto.helpText(), dto.sortOrder(), dto.createdDate(), dto.enabled());
    }

    private static ConfiguratorEffectResponse toResponse(ConfiguratorEffectDto dto) {
        return new ConfiguratorEffectResponse(dto.id(), dto.optionId(), dto.questionId(),
                dto.catalogItemId(), dto.effect().name(), dto.quantity(), dto.createdDate(),
                dto.enabled());
    }
}
