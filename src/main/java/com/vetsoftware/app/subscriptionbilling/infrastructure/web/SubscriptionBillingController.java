package com.vetsoftware.app.subscriptionbilling.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionbilling.application.port.in.FindBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.FindSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentsUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListSubscriptionChargesUseCase;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.response.BillingDocumentResponse;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.response.SubscriptionChargeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la clínica ve de su propia facturación: <b>solo lectura</b>.
 *
 * <p>
 * No hay aquí ningún endpoint que mueva dinero, y es una decisión de diseño, no
 * una funcionalidad pendiente: quien factura es la plataforma, no el cliente.
 * Un empleado del tenant no puede devengarse cargos, ni emitirse su propia
 * cuenta de cobro, ni tocar el saldo, igual que un cliente no se emite la
 * factura de su proveedor. Todo eso vive en
 * {@link SystemSubscriptionBillingController}, cerrado a {@code ROLE_SYSTEM}.
 *
 * <p>
 * <b>La empresa la pone el backend</b> con {@code authz.currentCompanyId()}: no
 * viaja en ninguna ruta ni en ningún cuerpo de este controller.
 */
@RestController
@RequestMapping("/subscription-billing")
public class SubscriptionBillingController {

    private final ListSubscriptionChargesUseCase listChargesUseCase;
    private final FindSubscriptionChargeUseCase findChargeUseCase;
    private final ListBillingDocumentsUseCase listDocumentsUseCase;
    private final FindBillingDocumentUseCase findDocumentUseCase;
    private final Authz authz;

    public SubscriptionBillingController(ListSubscriptionChargesUseCase listChargesUseCase,
            FindSubscriptionChargeUseCase findChargeUseCase,
            ListBillingDocumentsUseCase listDocumentsUseCase,
            FindBillingDocumentUseCase findDocumentUseCase, Authz authz) {
        this.listChargesUseCase = listChargesUseCase;
        this.findChargeUseCase = findChargeUseCase;
        this.listDocumentsUseCase = listDocumentsUseCase;
        this.findDocumentUseCase = findDocumentUseCase;
        this.authz = authz;
    }

    /** Lo devengado de la clínica. Los dos filtros son opcionales. */
    @GetMapping("/charges")
    public PageResponse<SubscriptionChargeResponse> listCharges(
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(required = false) ChargeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listChargesUseCase.listByCompany(authz.currentCompanyId(),
                subscriptionId, status, page, pageSize), SubscriptionChargeResponse::from);
    }

    @GetMapping("/charges/{id}")
    public SubscriptionChargeResponse findCharge(@PathVariable Long id) {
        return SubscriptionChargeResponse
                .from(findChargeUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping("/documents")
    public PageResponse<BillingDocumentResponse> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listDocumentsUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                BillingDocumentResponse::from);
    }

    @GetMapping("/documents/{id}")
    public BillingDocumentResponse findDocument(@PathVariable Long id) {
        return BillingDocumentResponse
                .from(findDocumentUseCase.findById(id, authz.currentCompanyId()));
    }
}
