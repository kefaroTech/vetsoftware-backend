package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ReverseBillingDocumentApplicationCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentSummaryDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListBillingDocumentApplicationsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReverseBillingDocumentApplicationUseCase;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.request.ApplyBillingDocumentRequest;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.response.BillingDocumentApplicationResponse;
import com.vetsoftware.app.subscriptionpayment.infrastructure.web.response.BillingDocumentSummary;
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
 * Que salda que.
 *
 * <p>
 * <strong>No hay {@code PUT} ni {@code DELETE}, y es la forma del recurso lo
 * que lo dice:</strong> una aplicacion no se edita ni se borra. Deshacerla es
 * un {@code POST} sobre {@code /reversal}, que crea otra fila que la
 * contra-aplica y deja las dos.
 */
@RestController
@RequestMapping("/billing-document-applications")
public class BillingDocumentApplicationController {

    private final ApplyBillingDocumentUseCase applyUseCase;
    private final ReverseBillingDocumentApplicationUseCase reverseUseCase;
    private final ListBillingDocumentApplicationsUseCase listUseCase;
    private final Authz authz;

    public BillingDocumentApplicationController(ApplyBillingDocumentUseCase applyUseCase,
            ReverseBillingDocumentApplicationUseCase reverseUseCase,
            ListBillingDocumentApplicationsUseCase listUseCase, Authz authz) {
        this.applyUseCase = applyUseCase;
        this.reverseUseCase = reverseUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDocumentApplicationResponse apply(
            @Valid @RequestBody ApplyBillingDocumentRequest request) {
        return toResponse(applyUseCase.execute(new ApplyBillingDocumentCommand(
                authz.currentCompanyId(), request.targetDocumentId(), request.sourceKind(),
                request.paymentId(), request.sourceDocumentId(), request.appliedAmount(),
                request.clientRequestId())));
    }

    @PostMapping("/{id}/reversal")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDocumentApplicationResponse reverse(@PathVariable Long id) {
        return toResponse(reverseUseCase.execute(
                new ReverseBillingDocumentApplicationCommand(id, authz.currentCompanyId())));
    }

    @GetMapping
    public PageResponse<BillingDocumentApplicationResponse> listByTargetDocument(
            @RequestParam Long targetDocumentId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByTargetDocument(targetDocumentId,
                authz.currentCompanyId(), page, pageSize), this::toResponse);
    }

    private BillingDocumentApplicationResponse toResponse(BillingDocumentApplicationDto dto) {
        return new BillingDocumentApplicationResponse(dto.id(), dto.companyId(),
                toSummary(dto.targetDocument()), dto.sourceKind(), dto.paymentId(),
                toSummary(dto.sourceDocument()), dto.appliedAmount(), dto.reversalOfId(),
                dto.appliedAt(), dto.createdDate());
    }

    private BillingDocumentSummary toSummary(BillingDocumentSummaryDto dto) {
        return dto == null
                ? null
                : new BillingDocumentSummary(dto.id(), dto.companyId(), dto.documentNumber(),
                        dto.documentKind(), dto.totalAmount(), dto.balanceAmount());
    }
}
