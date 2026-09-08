package com.vetsoftware.app.quote.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.quote.application.command.PurchaseModulesCommand;
import com.vetsoftware.app.quote.application.port.in.PurchaseModulesUseCase;
import com.vetsoftware.app.quote.infrastructure.web.request.ModulePurchaseRequest;
import com.vetsoftware.app.quote.infrastructure.web.response.ModulePurchaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Compra desde el escaparate de módulos del tenant. Vive aparte de
 * {@link QuoteController} porque la ruta pública es
 * {@code /subscriptions/modules/*} y no {@code /quotes/*}: es la contraparte de
 * escritura de {@code GET /subscriptions/modules}.
 */
@RestController
@RequestMapping("/subscriptions/modules")
public class ModulePurchaseController {

    private final PurchaseModulesUseCase purchaseUseCase;
    private final Authz authz;

    public ModulePurchaseController(PurchaseModulesUseCase purchaseUseCase, Authz authz) {
        this.purchaseUseCase = purchaseUseCase;
        this.authz = authz;
    }

    @PostMapping("/purchase")
    public ModulePurchaseResponse purchase(@Valid @RequestBody ModulePurchaseRequest request,
            HttpServletRequest httpRequest) {
        return ModulePurchaseResponse
                .from(purchaseUseCase.execute(new PurchaseModulesCommand(authz.currentCompanyId(),
                        authz.currentEmployeeId(), request.catalogItemCodes(),
                        request.billingCycle(), request.paymentSourceId(),
                        request.clientRequestId(), httpRequest.getRemoteAddr())));
    }
}
