package com.vetsoftware.app.subscriptionbilling.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscriptionbilling.application.command.CreateBillingDocumentSequenceCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateBillingDocumentSequenceUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListBillingDocumentSequencesUseCase;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.request.CreateBillingDocumentSequenceRequest;
import com.vetsoftware.app.subscriptionbilling.infrastructure.web.response.BillingDocumentSequenceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El consecutivo interno de los documentos de cobro.
 *
 * <p>
 * <b>Sin empresa en ninguna ruta</b>, porque {@code billing_document_sequences}
 * es un contador global de plataforma: es la única tabla de este slice sin
 * tenant, y sus dos puertos están cerrados a {@code hasRole("SYSTEM")} a secas.
 *
 * <p>
 * No hay endpoint para modificar ni borrar una serie, y no es un olvido: mover
 * a mano el {@code next_value} de un consecutivo en uso es la forma de repetir
 * un número ya emitido.
 */
@RestController
@RequestMapping("/system/billing-document-sequences")
public class BillingDocumentSequenceController {

    private final CreateBillingDocumentSequenceUseCase createUseCase;
    private final ListBillingDocumentSequencesUseCase listUseCase;

    public BillingDocumentSequenceController(CreateBillingDocumentSequenceUseCase createUseCase,
            ListBillingDocumentSequencesUseCase listUseCase) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDocumentSequenceResponse create(
            @Valid @RequestBody CreateBillingDocumentSequenceRequest request) {
        return BillingDocumentSequenceResponse.from(
                createUseCase.execute(new CreateBillingDocumentSequenceCommand(request.prefix())));
    }

    @GetMapping
    public PageResponse<BillingDocumentSequenceResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                BillingDocumentSequenceResponse::from);
    }
}
