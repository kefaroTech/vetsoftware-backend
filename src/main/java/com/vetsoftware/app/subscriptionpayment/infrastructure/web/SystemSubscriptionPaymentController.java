package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListAllSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.response.SubscriptionPaymentResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tesorería cross-tenant de la plataforma; no expone mutaciones tenant. */
@RestController
@RequestMapping("/system/subscription-payments")
public class SystemSubscriptionPaymentController {

    private final ListAllSubscriptionPaymentsUseCase listUseCase;

    public SystemSubscriptionPaymentController(ListAllSubscriptionPaymentsUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping
    public PageResponse<SubscriptionPaymentResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(companyId, page, pageSize),
                SubscriptionPaymentResponse::from);
    }
}
