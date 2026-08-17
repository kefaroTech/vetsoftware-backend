package com.vetsoftware.app.electronicdocument.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.command.ConvertPosToInvoiceCommand;
import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.electronicdocument.application.command.IssueDebitNoteCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.electronicdocument.application.port.in.BuildElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.ConvertPosToInvoiceUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentByAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.IssueCreditNoteUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.IssueDebitNoteUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.ListElectronicDocumentsUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.TransmitElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.infrastructure.web.request.BuildElectronicDocumentRequest;
import com.vetsoftware.app.electronicdocument.infrastructure.web.request.IssueCreditNoteRequest;
import com.vetsoftware.app.electronicdocument.infrastructure.web.request.IssueDebitNoteRequest;
import com.vetsoftware.app.electronicdocument.infrastructure.web.request.RegisterPosSaleRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * F2 - lectura del documento electronico + construccion PROVISIONAL desde una
 * cuenta cerrada. Sin update/delete (inmutabilidad fiscal). El POST de
 * construccion es el punto de entrada temporal para probar el modelo; F4 lo
 * reemplazara por la emision automatica al cerrar la cuenta.
 */
@RestController
@RequestMapping("/electronic-documents")
public class ElectronicDocumentController {
    private final BuildElectronicDocumentFromAccountUseCase buildUseCase;
    private final EmitElectronicDocumentFromAccountUseCase emitUseCase;
    private final RegisterPosSaleUseCase registerPosSaleUseCase;
    private final ConvertPosToInvoiceUseCase convertPosUseCase;
    private final TransmitElectronicDocumentUseCase transmitUseCase;
    private final IssueCreditNoteUseCase creditNoteUseCase;
    private final IssueDebitNoteUseCase debitNoteUseCase;
    private final FindElectronicDocumentUseCase findUseCase;
    private final FindElectronicDocumentByAccountUseCase findByAccountUseCase;
    private final ListElectronicDocumentsUseCase listUseCase;
    private final Authz authz;

    public ElectronicDocumentController(BuildElectronicDocumentFromAccountUseCase buildUseCase,
            EmitElectronicDocumentFromAccountUseCase emitUseCase,
            RegisterPosSaleUseCase registerPosSaleUseCase,
            ConvertPosToInvoiceUseCase convertPosUseCase,
            TransmitElectronicDocumentUseCase transmitUseCase,
            IssueCreditNoteUseCase creditNoteUseCase, IssueDebitNoteUseCase debitNoteUseCase,
            FindElectronicDocumentUseCase findUseCase,
            FindElectronicDocumentByAccountUseCase findByAccountUseCase,
            ListElectronicDocumentsUseCase listUseCase, Authz authz) {
        this.buildUseCase = buildUseCase;
        this.emitUseCase = emitUseCase;
        this.registerPosSaleUseCase = registerPosSaleUseCase;
        this.convertPosUseCase = convertPosUseCase;
        this.transmitUseCase = transmitUseCase;
        this.creditNoteUseCase = creditNoteUseCase;
        this.debitNoteUseCase = debitNoteUseCase;
        this.findUseCase = findUseCase;
        this.findByAccountUseCase = findByAccountUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping("/from-account")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto buildFromAccount(
            @Valid @RequestBody BuildElectronicDocumentRequest request) {
        return buildUseCase.execute(new BuildElectronicDocumentCommand(request.openAccountId(),
                request.documentType(), authz.currentCompanyId(), request.finalConsumer()));
    }

    /**
     * F4: emite end-to-end (construir + transmitir + entregar PDF/QR/correo) desde
     * una cuenta cerrada.
     */
    @PostMapping("/emit")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto emit(@Valid @RequestBody BuildElectronicDocumentRequest request) {
        return emitUseCase.execute(new EmitElectronicDocumentCommand(request.openAccountId(),
                request.documentType(), authz.currentCompanyId(), request.finalConsumer()));
    }

    /**
     * Registra una venta de POS como documento electrónico (sin cuenta abierta).
     * Con BILLING la transmite a la DIAN; sin el módulo queda PENDIENTE (datos
     * guardados, emisión diferida). Disponible para cualquier usuario con acceso al
     * POS (product.read) — no requiere permisos de facturación electrónica.
     */
    @PostMapping("/from-sale")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto registerPosSale(
            @Valid @RequestBody RegisterPosSaleRequest request) {
        List<RegisterPosSaleCommand.SaleLine> lines = request.lines().stream()
                .map(l -> new RegisterPosSaleCommand.SaleLine(l.kind(), l.refId(), l.description(),
                        l.quantity(), l.unitPrice()))
                .toList();
        List<RegisterPosSaleCommand.SalePayment> payments = request.payments().stream()
                .map(p -> new RegisterPosSaleCommand.SalePayment(p.means(), p.amount())).toList();
        return registerPosSaleUseCase.execute(new RegisterPosSaleCommand(authz.currentCompanyId(),
                request.documentType(), request.finalConsumer(), request.customerOwnerId(), lines,
                payments, request.clientRequestId(), authz.currentEmployeeId(),
                authz.resolveAccessibleBranch(request.branchId())));
    }

    /**
     * F4: convierte un documento equivalente POS en factura electrónica de venta.
     */
    @PostMapping("/{id}/convert-to-invoice")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto convertToInvoice(@PathVariable Long id) {
        return convertPosUseCase
                .execute(new ConvertPosToInvoiceCommand(id, authz.currentCompanyId()));
    }

    @PostMapping("/{id}/transmit")
    public ElectronicDocumentDto transmit(@PathVariable Long id) {
        return transmitUseCase
                .execute(new TransmitElectronicDocumentCommand(id, authz.currentCompanyId()));
    }

    /**
     * F5: emite una nota crédito total (anulación) que corrige la factura {id} y la
     * transmite.
     */
    @PostMapping("/{id}/credit-note")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto creditNote(@PathVariable Long id,
            @Valid @RequestBody IssueCreditNoteRequest request) {
        return creditNoteUseCase.execute(new IssueCreditNoteCommand(id, request.reason(),
                authz.currentCompanyId(), authz.currentEmployeeId(), request.partialAmount()));
    }

    /**
     * F5: emite una nota débito (aumento) que referencia la factura {id} y la
     * transmite.
     */
    @PostMapping("/{id}/debit-note")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto debitNote(@PathVariable Long id,
            @Valid @RequestBody IssueDebitNoteRequest request) {
        return debitNoteUseCase.execute(new IssueDebitNoteCommand(id, request.reason(),
                authz.currentCompanyId(), authz.currentEmployeeId(), request.additionalAmount()));
    }

    @GetMapping
    public PageResponse<ElectronicDocumentDto> listAll(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(required = false) ElectronicDocumentType documentType,
            @RequestParam(required = false) DianStatus dianStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByCompany(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId), documentType, dianStatus, page, pageSize));
    }

    @GetMapping("/{id}")
    public ElectronicDocumentDto findById(@PathVariable Long id) {
        return findUseCase.findById(id, authz.currentCompanyId());
    }

    /**
     * Documento emitido al cerrar una cuenta (para imprimir su recibo). 404 si la
     * cuenta no generó documento.
     */
    @GetMapping("/by-account/{openAccountId}")
    public ElectronicDocumentDto findByAccount(@PathVariable Long openAccountId) {
        return findByAccountUseCase.findByOpenAccount(openAccountId, authz.currentCompanyId())
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(openAccountId));
    }
}
