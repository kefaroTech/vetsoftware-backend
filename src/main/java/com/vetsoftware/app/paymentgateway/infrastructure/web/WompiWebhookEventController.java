package com.vetsoftware.app.paymentgateway.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentgateway.application.port.in.ListWompiWebhookEventsUseCase;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.paymentgateway.infrastructure.web.response.WompiWebhookEventResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/payment-gateway/wompi/events")
public class WompiWebhookEventController {

    private final ListWompiWebhookEventsUseCase listUseCase;

    public WompiWebhookEventController(ListWompiWebhookEventsUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping
    public PageResponse<WompiWebhookEventResponse> search(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.search(
                new ListWompiWebhookEventsQuery(reference, companyId, from, to, page, pageSize)),
                WompiWebhookEventResponse::from);
    }
}
