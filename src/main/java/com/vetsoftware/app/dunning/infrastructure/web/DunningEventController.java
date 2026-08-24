package com.vetsoftware.app.dunning.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.dunning.application.command.RecordDunningEventCommand;
import com.vetsoftware.app.dunning.application.port.in.FindDunningEventUseCase;
import com.vetsoftware.app.dunning.application.port.in.ListDunningEventsBySubscriptionUseCase;
import com.vetsoftware.app.dunning.application.port.in.RecordDunningEventUseCase;
import com.vetsoftware.app.dunning.infrastructure.web.request.RecordDunningEventRequest;
import com.vetsoftware.app.dunning.infrastructure.web.response.DunningEventResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El expediente de cobranza.
 *
 * <p>
 * <strong>Solo {@code POST} y {@code GET}, y esa ausencia es la
 * politica.</strong> No hay {@code PUT} ni {@code DELETE} porque una bitacora
 * que se puede reescribir u ocultar no demuestra nada, y demostrar que se aviso
 * antes de restringir la cuenta es su unica razon de existir.
 */
@RestController
@RequestMapping("/dunning-events")
public class DunningEventController {

    private final RecordDunningEventUseCase recordUseCase;
    private final FindDunningEventUseCase findUseCase;
    private final ListDunningEventsBySubscriptionUseCase listUseCase;
    private final Authz authz;

    public DunningEventController(RecordDunningEventUseCase recordUseCase,
            FindDunningEventUseCase findUseCase, ListDunningEventsBySubscriptionUseCase listUseCase,
            Authz authz) {
        this.recordUseCase = recordUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DunningEventResponse record(@Valid @RequestBody RecordDunningEventRequest request) {
        return DunningEventResponse.from(recordUseCase.execute(
                new RecordDunningEventCommand(authz.currentCompanyId(), request.subscriptionId(),
                        request.billingDocumentId(), request.eventType(), request.daysOverdue(),
                        request.channel(), request.detail(), request.occurredAt())));
    }

    @GetMapping("/{id}")
    public DunningEventResponse findById(@PathVariable Long id) {
        return DunningEventResponse.from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<DunningEventResponse> listBySubscription(@RequestParam Long subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listBySubscription(subscriptionId,
                authz.currentCompanyId(), page, pageSize), DunningEventResponse::from);
    }
}
