package com.vetsoftware.app.supplierwithholding.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.supplierwithholding.application.command.IssueSupplierWithholdingCertificateCommand;
import com.vetsoftware.app.supplierwithholding.application.command.PracticeSupplierWithholdingCommand;
import com.vetsoftware.app.supplierwithholding.application.command.RegisterSupplierWithholdingPaymentCommand;
import com.vetsoftware.app.supplierwithholding.application.port.in.FindSupplierWithholdingUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.IssueSupplierWithholdingCertificateUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.ListSupplierWithholdingsUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.PracticeSupplierWithholdingUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.RegisterSupplierWithholdingPaymentUseCase;
import com.vetsoftware.app.supplierwithholding.infrastructure.web.request.IssueSupplierWithholdingCertificateRequest;
import com.vetsoftware.app.supplierwithholding.infrastructure.web.request.PracticeSupplierWithholdingRequest;
import com.vetsoftware.app.supplierwithholding.infrastructure.web.request.RegisterSupplierWithholdingPaymentRequest;
import com.vetsoftware.app.supplierwithholding.infrastructure.web.response.SupplierWithholdingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las retenciones que Lumbre le practica a sus proveedores, y <strong>solo
 * desde la consola de plataforma</strong>.
 *
 * <p>
 * <strong>No hay controller de tenant.</strong> Es una obligacion fiscal de la
 * plataforma como agente de retencion; una clinica no tiene nada que consultar
 * aqui. Los cinco puertos van cerrados a {@code hasRole('SYSTEM')} a secas
 * ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
 *
 * <p>
 * <strong>No hay endpoint de borrado ni de edicion de los importes.</strong>
 * Una retencion practicada es un hecho: si estuvo mal calculada, lo que
 * corresponde es corregir la <em>declaracion</em> del periodo, no reescribir el
 * soporte. Lo unico que se escribe despues son los dos documentos que llegan
 * tarde: el certificado del proveedor y el acuse de la consignacion.
 */
@RestController
@RequestMapping("/system/supplier-withholdings")
public class SystemSupplierWithholdingController {

    private final PracticeSupplierWithholdingUseCase practiceUseCase;
    private final IssueSupplierWithholdingCertificateUseCase issueCertificateUseCase;
    private final RegisterSupplierWithholdingPaymentUseCase registerPaymentUseCase;
    private final FindSupplierWithholdingUseCase findUseCase;
    private final ListSupplierWithholdingsUseCase listUseCase;

    public SystemSupplierWithholdingController(PracticeSupplierWithholdingUseCase practiceUseCase,
            IssueSupplierWithholdingCertificateUseCase issueCertificateUseCase,
            RegisterSupplierWithholdingPaymentUseCase registerPaymentUseCase,
            FindSupplierWithholdingUseCase findUseCase,
            ListSupplierWithholdingsUseCase listUseCase) {
        this.practiceUseCase = practiceUseCase;
        this.issueCertificateUseCase = issueCertificateUseCase;
        this.registerPaymentUseCase = registerPaymentUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierWithholdingResponse practice(
            @Valid @RequestBody PracticeSupplierWithholdingRequest request) {
        return SupplierWithholdingResponse
                .from(practiceUseCase.execute(new PracticeSupplierWithholdingCommand(
                        request.supplierTaxId(), request.supplierName(), request.supplierDocType(),
                        request.supplierInvoiceRef(), request.withholdingType(), request.concept(),
                        request.taxableBase(), request.ratePercent(), request.amount(),
                        request.municipalityCode(), request.fiscalYear(), request.fiscalPeriodKey(),
                        request.practicedOn())));
    }

    /**
     * Emite el certificado del proveedor. La fecha la pone el caso de uso con su
     * reloj inyectado, y una segunda emision se rechaza: ese numero ya esta en
     * manos del proveedor.
     */
    @PatchMapping("/{id}/certificate")
    public SupplierWithholdingResponse issueCertificate(@PathVariable Long id,
            @Valid @RequestBody IssueSupplierWithholdingCertificateRequest request) {
        return SupplierWithholdingResponse.from(issueCertificateUseCase.execute(
                new IssueSupplierWithholdingCertificateCommand(id, request.certificateRef())));
    }

    @PatchMapping("/{id}/payment-receipt")
    public SupplierWithholdingResponse registerPayment(@PathVariable Long id,
            @Valid @RequestBody RegisterSupplierWithholdingPaymentRequest request) {
        return SupplierWithholdingResponse.from(registerPaymentUseCase.execute(
                new RegisterSupplierWithholdingPaymentCommand(id, request.paymentReceiptRef())));
    }

    @GetMapping("/{id}")
    public SupplierWithholdingResponse findById(@PathVariable Long id) {
        return SupplierWithholdingResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<SupplierWithholdingResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                SupplierWithholdingResponse::from);
    }

    /**
     * <strong>La declaracion del mes</strong>: lo retenido en un periodo fiscal.
     */
    @GetMapping("/by-period/{fiscalPeriodKey}")
    public PageResponse<SupplierWithholdingResponse> listByPeriod(
            @PathVariable String fiscalPeriodKey, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByFiscalPeriod(fiscalPeriodKey, page, pageSize),
                SupplierWithholdingResponse::from);
    }

    /**
     * <strong>El certificado anual del proveedor</strong>: lo retenido a un NIT en
     * un año gravable. Es el documento que hay obligacion de entregarle.
     */
    @GetMapping("/by-supplier/{supplierTaxId}")
    public PageResponse<SupplierWithholdingResponse> listBySupplier(
            @PathVariable String supplierTaxId, @RequestParam int fiscalYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listBySupplierAndYear(supplierTaxId, fiscalYear, page, pageSize),
                SupplierWithholdingResponse::from);
    }
}
