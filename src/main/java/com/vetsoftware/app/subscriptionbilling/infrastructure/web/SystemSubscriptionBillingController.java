package com.vetsoftware.app.subscriptionbilling.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.RegisterExternalInvoiceCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.SubmitBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.VoidSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.GenerateBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueCreditNoteUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentsAwaitingExternalUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListOverdueBillingDocumentsUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.RegisterExternalInvoiceUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.SubmitBillingDocumentForExternalIssueUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.request.CreateSubscriptionChargeRequest;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.request.GenerateBillingDocumentRequest;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.request.IssueCreditNoteRequest;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.request.RegisterExternalInvoiceRequest;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.request.VoidSubscriptionChargeRequest;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.response.BillingDocumentResponse;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.response.SubscriptionChargeResponse;
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
 * La consola de facturación de la plataforma: todo lo que <b>mueve</b> la capa
 * de dinero.
 *
 * <p>
 * <b>Cada endpoint de escritura cuelga de {@code /companies/{companyId}} y esa
 * es la única vía por la que entra la empresa.</b> No viaja en ningún cuerpo
 * —lo prohíbe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, y con motivo: un
 * {@code companyId} escrito por el cliente en el JSON convierte cualquier
 * validación de propiedad en una comparación del número consigo mismo—. Aquí sí
 * puede llegar por la ruta porque el gate es {@code hasRole("SYSTEM")} a secas:
 * el principal es cross-tenant por diseño y elegir empresa es precisamente lo
 * que tiene que poder hacer.
 *
 * <p>
 * Los dos listados de abajo <b>no llevan empresa</b>: son barridos de
 * plataforma y por eso solo los sirve SYSTEM
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
 */
@RestController
@RequestMapping("/system/subscription-billing")
public class SystemSubscriptionBillingController {

    private final CreateSubscriptionChargeUseCase createChargeUseCase;
    private final VoidSubscriptionChargeUseCase voidChargeUseCase;
    private final GenerateBillingDocumentUseCase generateUseCase;
    private final SubmitBillingDocumentForExternalIssueUseCase submitUseCase;
    private final RegisterExternalInvoiceUseCase registerExternalUseCase;
    private final VoidBillingDocumentUseCase voidDocumentUseCase;
    private final IssueCreditNoteUseCase creditNoteUseCase;
    private final ListBillingDocumentsAwaitingExternalUseCase awaitingUseCase;
    private final ListOverdueBillingDocumentsUseCase overdueUseCase;
    private final Authz authz;

    public SystemSubscriptionBillingController(CreateSubscriptionChargeUseCase createChargeUseCase,
            VoidSubscriptionChargeUseCase voidChargeUseCase,
            GenerateBillingDocumentUseCase generateUseCase,
            SubmitBillingDocumentForExternalIssueUseCase submitUseCase,
            RegisterExternalInvoiceUseCase registerExternalUseCase,
            VoidBillingDocumentUseCase voidDocumentUseCase,
            IssueCreditNoteUseCase creditNoteUseCase,
            ListBillingDocumentsAwaitingExternalUseCase awaitingUseCase,
            ListOverdueBillingDocumentsUseCase overdueUseCase, Authz authz) {
        this.createChargeUseCase = createChargeUseCase;
        this.voidChargeUseCase = voidChargeUseCase;
        this.generateUseCase = generateUseCase;
        this.submitUseCase = submitUseCase;
        this.registerExternalUseCase = registerExternalUseCase;
        this.voidDocumentUseCase = voidDocumentUseCase;
        this.creditNoteUseCase = creditNoteUseCase;
        this.awaitingUseCase = awaitingUseCase;
        this.overdueUseCase = overdueUseCase;
        this.authz = authz;
    }

    /** Devenga un cargo contra el contrato de una clínica. */
    @PostMapping("/companies/{companyId}/charges")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionChargeResponse createCharge(@PathVariable Long companyId,
            @Valid @RequestBody CreateSubscriptionChargeRequest request) {
        return SubscriptionChargeResponse
                .from(createChargeUseCase.execute(new CreateSubscriptionChargeCommand(companyId,
                        request.subscriptionId(), request.subscriptionItemId(),
                        request.chargeType(), request.description(), request.servicePeriodStart(),
                        request.servicePeriodEnd(), request.quantity(), request.unitAmount(),
                        request.subtotalAmount(), request.taxRate(), request.taxTreatment(),
                        request.prorationDays(), request.periodDays(), request.amendmentId())));
    }

    /**
     * Anula un cargo creando el que lo compensa. Devuelve <b>el cargo nuevo</b>: el
     * original sigue ahí, marcado {@code VOIDED}, y los dos suman cero.
     */
    @PostMapping("/companies/{companyId}/charges/{id}/void")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionChargeResponse voidCharge(@PathVariable Long companyId,
            @PathVariable Long id, @Valid @RequestBody VoidSubscriptionChargeRequest request) {
        return SubscriptionChargeResponse.from(voidChargeUseCase
                .execute(new VoidSubscriptionChargeCommand(id, companyId, request.description())));
    }

    @PostMapping("/companies/{companyId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDocumentResponse generate(@PathVariable Long companyId,
            @Valid @RequestBody GenerateBillingDocumentRequest request) {
        return BillingDocumentResponse.from(generateUseCase
                .execute(new GenerateBillingDocumentCommand(companyId, request.subscriptionId(),
                        request.billingReason(), request.periodStart(), request.periodEnd())));
    }

    @PostMapping("/companies/{companyId}/documents/{id}/await-external")
    public BillingDocumentResponse submitForExternalIssue(@PathVariable Long companyId,
            @PathVariable Long id) {
        return BillingDocumentResponse
                .from(submitUseCase.execute(new SubmitBillingDocumentCommand(id, companyId)));
    }

    /**
     * Captura la referencia de la factura emitida fuera. Con ella se fija el
     * vencimiento, contado <b>desde la fecha fiscal</b>.
     *
     * <p>
     * <b>Quién la registra lo pone el backend</b>, con
     * {@code authz.currentSystemUserId()} y no desde el cuerpo. Es el rastro del
     * paso manual y lo que permite reclamarlo cuando no se hizo: si el id llegara
     * en el JSON, quien captura la referencia elegiría a quién atribuírsela. Se usa
     * la variante que lanza y no la opcional a propósito — un documento marcado
     * como facturado que nadie registró es exactamente el cobro que desaparece del
     * radar.
     */
    @PostMapping("/companies/{companyId}/documents/{id}/external-invoice")
    public BillingDocumentResponse registerExternalInvoice(@PathVariable Long companyId,
            @PathVariable Long id, @Valid @RequestBody RegisterExternalInvoiceRequest request) {
        return BillingDocumentResponse
                .from(registerExternalUseCase.execute(new RegisterExternalInvoiceCommand(id,
                        companyId, request.invoiceNumber(), request.cufe(), request.issuedAt(),
                        request.provider(), authz.currentSystemUserId())));
    }

    @PostMapping("/companies/{companyId}/documents/{id}/void")
    public BillingDocumentResponse voidDocument(@PathVariable Long companyId,
            @PathVariable Long id) {
        return BillingDocumentResponse
                .from(voidDocumentUseCase.execute(new VoidBillingDocumentCommand(id, companyId)));
    }

    /**
     * Emite la nota crédito que corrige el documento {@code id}. Es el único camino
     * para corregir uno con factura externa ya registrada.
     */
    @PostMapping("/companies/{companyId}/documents/{id}/credit-note")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDocumentResponse issueCreditNote(@PathVariable Long companyId,
            @PathVariable Long id, @Valid @RequestBody IssueCreditNoteRequest request) {
        return BillingDocumentResponse.from(creditNoteUseCase
                .execute(new IssueCreditNoteCommand(companyId, id, request.chargeIds())));
    }

    /** La lista de trabajo pendiente de cada mes, de todas las clínicas. */
    @GetMapping("/documents/awaiting-external")
    public PageResponse<BillingDocumentResponse> listAwaitingExternal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(awaitingUseCase.listAwaitingExternal(page, pageSize),
                BillingDocumentResponse::from);
    }

    /** El barrido de mora de la plataforma. */
    @GetMapping("/documents/overdue")
    public PageResponse<BillingDocumentResponse> listOverdue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(overdueUseCase.listOverdue(page, pageSize),
                BillingDocumentResponse::from);
    }
}
