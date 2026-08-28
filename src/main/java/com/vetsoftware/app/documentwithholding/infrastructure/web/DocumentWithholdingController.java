package com.vetsoftware.app.documentwithholding.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.documentwithholding.application.port.in.FindDocumentWithholdingUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListUncertifiedDocumentWithholdingsByCompanyUseCase;
import com.vetsoftware.app.documentwithholding.infrastructure.web.response.DocumentWithholdingResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant de las retenciones, y es <strong>solo de lectura</strong>.
 *
 * <p>
 * No es que las escrituras aun no esten hechas: no van aqui. Registrar una
 * retencion es declarar que una factura quedo saldada por un importe que nunca
 * entro a la caja, asi que vive en {@link SystemDocumentWithholdingController}
 * y este controller no tiene un solo {@code @PostMapping}. Un endpoint de
 * escritura aqui seria una clinica dandose por pagada a si misma.
 *
 * <p>
 * Lo que si le corresponde al cliente es <strong>ver lo suyo y reclamar lo que
 * le falta</strong>: la retencion es plata propia que fue a la DIAN a su
 * nombre, y sin el certificado no la puede imputar.
 *
 * <p>
 * La empresa sale siempre de {@code authz.currentCompanyId()} y nunca de la URL
 * ni del cuerpo: es lo que impide leer las retenciones de otra clinica
 * escribiendo su id.
 */
@RestController
@RequestMapping("/document-withholdings")
public class DocumentWithholdingController {

    private final FindDocumentWithholdingUseCase findUseCase;
    private final ListDocumentWithholdingsUseCase listUseCase;
    private final ListUncertifiedDocumentWithholdingsByCompanyUseCase listUncertifiedUseCase;
    private final Authz authz;

    public DocumentWithholdingController(FindDocumentWithholdingUseCase findUseCase,
            ListDocumentWithholdingsUseCase listUseCase,
            ListUncertifiedDocumentWithholdingsByCompanyUseCase listUncertifiedUseCase,
            Authz authz) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listUncertifiedUseCase = listUncertifiedUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public DocumentWithholdingResponse findById(@PathVariable Long id) {
        return DocumentWithholdingResponse.from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<DocumentWithholdingResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                DocumentWithholdingResponse::from);
    }

    /**
     * Lo que le retuvieron en un ano y aun no le han certificado: la lista con la
     * que el cliente reclama.
     *
     * <p>
     * El ano es obligatorio y no tiene valor por defecto <strong>a
     * proposito</strong>. Un defecto silencioso —el ano en curso, por ejemplo—
     * dejaria la bandeja del ano anterior invisible justo en enero, que es cuando
     * hay que reclamarlo antes de que venza el plazo de marzo.
     */
    @GetMapping("/uncertified")
    public PageResponse<DocumentWithholdingResponse> listUncertified(@RequestParam int fiscalYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUncertifiedUseCase
                .listUncertifiedByCompany(authz.currentCompanyId(), fiscalYear, page, pageSize),
                DocumentWithholdingResponse::from);
    }
}
