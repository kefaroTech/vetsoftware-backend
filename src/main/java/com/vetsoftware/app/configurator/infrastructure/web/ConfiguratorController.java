package com.vetsoftware.app.configurator.infrastructure.web;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.configurator.application.dto.QuestionnaireQuestionDto;
import com.vetsoftware.app.configurator.application.port.in.GetPublicQuestionnaireUseCase;
import com.vetsoftware.app.configurator.application.port.in.ResolveConfiguratorSelectionUseCase;
import com.vetsoftware.app.configurator.infrastructure.web.request.ResolveConfiguratorSelectionRequest;
import com.vetsoftware.app.configurator.infrastructure.web.response.ConfiguratorSelectionResponse;
import com.vetsoftware.app.configurator.infrastructure.web.response.QuestionnaireOptionResponse;
import com.vetsoftware.app.configurator.infrastructure.web.response.QuestionnaireQuestionResponse;
import com.vetsoftware.app.configurator.infrastructure.web.response.SelectedItemResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara del asistente de venta. Separada del controller de administración
 * porque las dos mitades tienen público distinto: aquí entra gente que todavía
 * no es cliente.
 *
 * <p>
 * <strong>Los dos endpoints son públicos</strong> y están declarados como tales
 * en {@code PublicRoutes.BUSINESS}, con patrones exactos y sin comodines: un
 * {@code /configurator/**} habría abierto de paso los endpoints de
 * administración de al lado.
 *
 * <p>
 * Que sean los dos no es simetría por gusto: son las dos mitades del mismo
 * asistente. El prospecto lee el cuestionario, lo responde, y la respuesta se
 * traduce en un carrito que se convierte en cotización. Abrir solo la primera
 * mitad deja un <strong>401 en el paso siguiente</strong> para exactamente el
 * público al que la primera se abrió — y como en desarrollo se prueba con
 * sesión abierta, ese 401 no aparece hasta la demo.
 *
 * <p>
 * El reparo que justificaba tenerlo cerrado era real y sigue en pie: un
 * {@code POST} anónimo es superficie pública sin límite de tasa. La respuesta
 * no es cerrar el endpoint sino ponerle el límite —
 * {@code CONFIGURATOR_RESOLVE_LIMIT} en {@code LoginRateLimitFilter}, 60/min
 * por IP—, que es lo que el repositorio ya exige de toda ruta pública
 * {@code POST} y lo que {@code LoginRateLimitFilterTest} comprueba recorriendo
 * {@code PublicRoutes.BUSINESS}.
 */
@RestController
@RequestMapping("/configurator")
public class ConfiguratorController {

    private final GetPublicQuestionnaireUseCase questionnaireUseCase;
    private final ResolveConfiguratorSelectionUseCase resolveUseCase;

    public ConfiguratorController(GetPublicQuestionnaireUseCase questionnaireUseCase,
            ResolveConfiguratorSelectionUseCase resolveUseCase) {
        this.questionnaireUseCase = questionnaireUseCase;
        this.resolveUseCase = resolveUseCase;
    }

    @GetMapping("/questionnaire")
    public List<QuestionnaireQuestionResponse> questionnaire() {
        return questionnaireUseCase.get().stream().map(ConfiguratorController::toResponse).toList();
    }

    @PostMapping("/resolve")
    public ConfiguratorSelectionResponse resolve(
            @Valid @RequestBody ResolveConfiguratorSelectionRequest request) {
        ConfiguratorSelectionDto selection = resolveUseCase
                .resolve(new ResolveConfiguratorSelectionCommand(request.selectedOptionIds(),
                        request.numericAnswers(), request.billingCycle()));
        return new ConfiguratorSelectionResponse(selection.items().stream()
                .map(item -> new SelectedItemResponse(item.code(), item.quantity())).toList());
    }

    private static QuestionnaireQuestionResponse toResponse(QuestionnaireQuestionDto dto) {
        return new QuestionnaireQuestionResponse(dto.id(), dto.code(), dto.questionText(),
                dto.helpText(), dto.answerType(), dto.parentOptionId(), dto.required(),
                dto.sortOrder(),
                dto.options().stream().map(option -> new QuestionnaireOptionResponse(option.id(),
                        option.code(), option.label(), option.helpText(), option.sortOrder()))
                        .toList());
    }
}
