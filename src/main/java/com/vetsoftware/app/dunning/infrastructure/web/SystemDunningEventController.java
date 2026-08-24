package com.vetsoftware.app.dunning.infrastructure.web;

import com.vetsoftware.app.dunning.application.port.in.ListAllDunningEventsUseCase;
import com.vetsoftware.app.dunning.infrastructure.web.response.DunningEventResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expediente de cobranza cross-tenant para la plataforma. */
@RestController
@RequestMapping("/system/dunning-events")
public class SystemDunningEventController {

    private final ListAllDunningEventsUseCase listUseCase;

    public SystemDunningEventController(ListAllDunningEventsUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping
    public PageResponse<DunningEventResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(companyId, page, pageSize),
                DunningEventResponse::from);
    }
}
