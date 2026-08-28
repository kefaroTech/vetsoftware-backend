package com.vetsoftware.app.withholdingcertificate.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.withholdingcertificate.application.port.in.FindWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListMissingWithholdingCertificatesByCompanyUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.infrastructure.web.response.WithholdingCertificateResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant de los certificados de retencion, y es <strong>solo de
 * lectura</strong>.
 *
 * <p>
 * No es que las escrituras aun no esten hechas: no van aqui. El certificado lo
 * expide un tercero -el cliente que retuvo- y lo registra tesoreria de la
 * plataforma al conciliar la cartera, asi que las tres escrituras viven en
 * {@link SystemWithholdingCertificateController} y este controller no tiene un
 * solo {@code @PostMapping} ni {@code @PatchMapping}. Un endpoint de escritura
 * aqui seria una clinica declarando por su cuenta cuanto le retuvieron.
 *
 * <p>
 * La empresa sale siempre de {@code authz.currentCompanyId()} y nunca de la URL
 * ni del cuerpo: es lo que impide leer los certificados de otra clinica
 * escribiendo su id.
 */
@RestController
@RequestMapping("/withholding-certificates")
public class WithholdingCertificateController {

    private final FindWithholdingCertificateUseCase findUseCase;
    private final ListWithholdingCertificatesUseCase listUseCase;
    private final ListMissingWithholdingCertificatesByCompanyUseCase listMissingUseCase;
    private final Authz authz;

    public WithholdingCertificateController(FindWithholdingCertificateUseCase findUseCase,
            ListWithholdingCertificatesUseCase listUseCase,
            ListMissingWithholdingCertificatesByCompanyUseCase listMissingUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listMissingUseCase = listMissingUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public WithholdingCertificateResponse findById(@PathVariable Long id) {
        return WithholdingCertificateResponse
                .from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<WithholdingCertificateResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                WithholdingCertificateResponse::from);
    }

    /**
     * Lo que a esta clinica le falta por recibir antes de que venza el plazo, que
     * es la razon por la que {@code legal_deadline_on} se guarda como dato.
     */
    @GetMapping("/missing")
    public PageResponse<WithholdingCertificateResponse> listMissing(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listMissingUseCase.listMissingByCompany(authz.currentCompanyId(),
                deadlineBefore, page, pageSize), WithholdingCertificateResponse::from);
    }
}
