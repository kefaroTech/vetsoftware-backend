package com.vetsoftware.app.documentwithholding.infrastructure.web;

import com.vetsoftware.app.documentwithholding.application.command.LinkWithholdingCertificateCommand;
import com.vetsoftware.app.documentwithholding.application.command.RegisterDocumentWithholdingCommand;
import com.vetsoftware.app.documentwithholding.application.port.in.LinkWithholdingCertificateUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListAllDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListUncertifiedDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.RegisterDocumentWithholdingUseCase;
import com.vetsoftware.app.documentwithholding.infrastructure.web.request.LinkWithholdingCertificateRequest;
import com.vetsoftware.app.documentwithholding.infrastructure.web.request.RegisterDocumentWithholdingRequest;
import com.vetsoftware.app.documentwithholding.infrastructure.web.response.DocumentWithholdingResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
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
 * Tesoreria de la plataforma: el unico sitio desde el que se registra una
 * retencion y desde el que se apunta a su certificado, mas el barrido de
 * vigilancia cross-tenant.
 *
 * <p>
 * Aqui el {@code companyId} <strong>viaja como {@code @RequestParam}</strong>,
 * no en el cuerpo y no desde el principal: un principal SYSTEM no tiene empresa
 * propia, es tesoreria eligiendo sobre que clinica escribe. En el cuerpo no
 * puede ir —lo prohibe la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que
 * mira todo {@code @RequestBody} sin mirar la ruta ni el rol—. La proteccion no
 * es que el servidor inyecte la empresa —no puede— sino que los dos casos de
 * uso de escritura estan cerrados a {@code hasRole('SYSTEM')} a secas.
 */
@RestController
@RequestMapping("/system/document-withholdings")
public class SystemDocumentWithholdingController {

    private final RegisterDocumentWithholdingUseCase registerUseCase;
    private final LinkWithholdingCertificateUseCase linkUseCase;
    private final ListAllDocumentWithholdingsUseCase listAllUseCase;
    private final ListUncertifiedDocumentWithholdingsUseCase listUncertifiedUseCase;

    public SystemDocumentWithholdingController(RegisterDocumentWithholdingUseCase registerUseCase,
            LinkWithholdingCertificateUseCase linkUseCase,
            ListAllDocumentWithholdingsUseCase listAllUseCase,
            ListUncertifiedDocumentWithholdingsUseCase listUncertifiedUseCase) {
        this.registerUseCase = registerUseCase;
        this.linkUseCase = linkUseCase;
        this.listAllUseCase = listAllUseCase;
        this.listUncertifiedUseCase = listUncertifiedUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentWithholdingResponse register(@RequestParam Long companyId,
            @Valid @RequestBody RegisterDocumentWithholdingRequest request) {
        return DocumentWithholdingResponse
                .from(registerUseCase.execute(new RegisterDocumentWithholdingCommand(companyId,
                        request.billingDocumentId(), request.type(), request.taxableBase(),
                        request.ratePercent(), request.amount(), request.municipalityCode(),
                        request.fiscalYear(), request.fiscalPeriodKey(), request.practicedOn())));
    }

    /**
     * Apunta la retencion a su certificado.
     *
     * <p>
     * {@code POST} y no {@code PATCH} porque no es una edicion parcial de la
     * retencion: es un hecho nuevo del expediente —llego el papel—, y la tabla solo
     * se agrega. La ruta lo dice como sub-recurso.
     */
    @PostMapping("/{id}/certificate")
    public DocumentWithholdingResponse linkCertificate(@PathVariable Long id,
            @RequestParam Long companyId,
            @Valid @RequestBody LinkWithholdingCertificateRequest request) {
        return DocumentWithholdingResponse.from(linkUseCase.execute(
                new LinkWithholdingCertificateCommand(id, companyId, request.certificateId())));
    }

    @GetMapping
    public PageResponse<DocumentWithholdingResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(companyId, page, pageSize),
                DocumentWithholdingResponse::from);
    }

    /**
     * El barrido de vigilancia: todo lo retenido en un ano que sigue sin
     * certificado, en todas las clinicas.
     *
     * <p>
     * <strong>No acepta {@code companyId}, ni siquiera opcional.</strong> Sirve a
     * un puerto cerrado a {@code hasRole('SYSTEM')} a secas; la version acotada por
     * empresa es otro caso de uso y vive en el controller de tenant, donde la
     * empresa la pone el token y no el que pregunta.
     */
    @GetMapping("/uncertified")
    public PageResponse<DocumentWithholdingResponse> listUncertified(@RequestParam int fiscalYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUncertifiedUseCase.listUncertified(fiscalYear, page, pageSize),
                DocumentWithholdingResponse::from);
    }
}
