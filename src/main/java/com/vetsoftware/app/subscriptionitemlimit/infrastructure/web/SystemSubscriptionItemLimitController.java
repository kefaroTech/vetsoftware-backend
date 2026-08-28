package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web;

import com.vetsoftware.app.subscriptionitemlimit.application.command.FreezeSubscriptionItemLimitCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.command.PropagateCatalogLimitImprovementCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.FreezeSubscriptionItemLimitUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.ListSubscriptionItemLimitsUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.PropagateCatalogLimitImprovementUseCase;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.request.FreezeSubscriptionItemLimitRequest;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.request.PropagateCatalogLimitImprovementRequest;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.response.LimitPropagationResponse;
import com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.response.SubscriptionItemLimitResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola de plataforma sobre los techos congelados: todo lo que los
 * <strong>mueve</strong>.
 *
 * <p>
 * <strong>La empresa entra por la ruta y solo por la ruta.</strong> No viaja en
 * ningún cuerpo —lo prohíbe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— y tampoco se
 * deriva del principal, porque un usuario de plataforma no tiene empresa:
 * {@code authz.currentCompanyId()} lanzaría {@code AccessDeniedException}.
 * Puede llegar por {@code @PathVariable} precisamente porque el gate es
 * {@code hasRole('SYSTEM')} a secas y elegir empresa es lo que ese principal
 * tiene que poder hacer; es la salida que la propia regla deja escrita.
 *
 * <p>
 * La propagación de mejoras <strong>no cuelga de ninguna empresa</strong>: es
 * una operación que cruza todos los contratos vivos por definición (D-75), y
 * por eso su ruta es plana.
 */
@RestController
@RequestMapping("/system/subscription-item-limits")
public class SystemSubscriptionItemLimitController {

    private final FreezeSubscriptionItemLimitUseCase freezeUseCase;
    private final PropagateCatalogLimitImprovementUseCase propagateUseCase;
    private final ListSubscriptionItemLimitsUseCase listUseCase;

    public SystemSubscriptionItemLimitController(FreezeSubscriptionItemLimitUseCase freezeUseCase,
            PropagateCatalogLimitImprovementUseCase propagateUseCase,
            ListSubscriptionItemLimitsUseCase listUseCase) {
        this.freezeUseCase = freezeUseCase;
        this.propagateUseCase = propagateUseCase;
        this.listUseCase = listUseCase;
    }

    /** Congela en la línea del contrato el techo que regía el día de la firma. */
    @PostMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionItemLimitResponse freeze(@PathVariable Long companyId,
            @Valid @RequestBody FreezeSubscriptionItemLimitRequest request) {
        return SubscriptionItemLimitResponse.from(freezeUseCase.execute(
                new FreezeSubscriptionItemLimitCommand(companyId, request.subscriptionItemId(),
                        request.limitDimensionId(), request.measureKind(), request.mode(),
                        request.limitQuantity(), request.resetPeriod(), request.enforcement(),
                        request.overageUnitAmount(), request.warnThreshold())));
    }

    /**
     * Los techos congelados de una clínica, vistos desde plataforma. Es el mismo
     * puerto que sirve a {@link SubscriptionItemLimitController}: su
     * {@code @PreAuthorize} admite {@code hasRole('SYSTEM')} <em>o</em> la propia
     * empresa, y aquí entra por la primera mitad.
     */
    @GetMapping("/companies/{companyId}")
    public List<SubscriptionItemLimitResponse> listByCompany(@PathVariable Long companyId) {
        return listUseCase.listByCompanyId(companyId).stream()
                .map(SubscriptionItemLimitResponse::from).toList();
    }

    /**
     * Propaga una mejora del cupo de fábrica a los contratos vivos. Devuelve
     * cuántos cambiaron de verdad; cero es una respuesta correcta.
     */
    @PostMapping("/propagations")
    public LimitPropagationResponse propagate(
            @Valid @RequestBody PropagateCatalogLimitImprovementRequest request) {
        return new LimitPropagationResponse(
                propagateUseCase.execute(new PropagateCatalogLimitImprovementCommand(
                        request.catalogItemId(), request.limitDimensionId(), request.factoryMode(),
                        request.factoryLimitQuantity())));
    }
}
