package com.vetsoftware.app.withholdingcertificate.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.withholdingcertificate.application.command.AttachSubstituteEvidenceCommand;
import com.vetsoftware.app.withholdingcertificate.application.command.ReceiveWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.command.RegisterWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.port.in.AttachSubstituteEvidenceUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListAllWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListMissingWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ReceiveWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.RegisterWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.infrastructure.web.request.AttachSubstituteEvidenceRequest;
import com.vetsoftware.app.withholdingcertificate.infrastructure.web.request.ReceiveWithholdingCertificateRequest;
import com.vetsoftware.app.withholdingcertificate.infrastructure.web.request.RegisterWithholdingCertificateRequest;
import com.vetsoftware.app.withholdingcertificate.infrastructure.web.response.WithholdingCertificateResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Tesoreria de la plataforma: el unico sitio desde el que se abre, se cierra y
 * se acredita un certificado de retencion, y el barrido cross-tenant de lo que
 * falta por recibir.
 *
 * <p>
 * En el registro, el {@code companyId} <strong>viaja como
 * {@code @RequestParam}</strong>, no en el cuerpo y no desde el principal: un
 * principal SYSTEM no tiene empresa propia, es tesoreria eligiendo de que
 * clinica es el certificado. En el cuerpo no puede ir -lo prohibe la regla dura
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que mira todo {@code @RequestBody} sin
 * mirar la ruta ni el rol-. La proteccion no es que el servidor inyecte la
 * empresa -no puede- sino que el caso de uso esta cerrado a
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * Las dos segundas escrituras <strong>no llevan empresa en ninguna
 * forma</strong>, ni parametro ni cuerpo: senalan la fila por su {@code id}, y
 * por eso sus puertos son {@code hasRole('SYSTEM')} a secas y no pueden ser
 * otra cosa ({@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}, BE-COV).
 */
@RestController
@RequestMapping("/system/withholding-certificates")
public class SystemWithholdingCertificateController {

    private final RegisterWithholdingCertificateUseCase registerUseCase;
    private final ReceiveWithholdingCertificateUseCase receiveUseCase;
    private final AttachSubstituteEvidenceUseCase attachSubstituteUseCase;
    private final ListAllWithholdingCertificatesUseCase listAllUseCase;
    private final ListMissingWithholdingCertificatesUseCase listMissingUseCase;

    public SystemWithholdingCertificateController(
            RegisterWithholdingCertificateUseCase registerUseCase,
            ReceiveWithholdingCertificateUseCase receiveUseCase,
            AttachSubstituteEvidenceUseCase attachSubstituteUseCase,
            ListAllWithholdingCertificatesUseCase listAllUseCase,
            ListMissingWithholdingCertificatesUseCase listMissingUseCase) {
        this.registerUseCase = registerUseCase;
        this.receiveUseCase = receiveUseCase;
        this.attachSubstituteUseCase = attachSubstituteUseCase;
        this.listAllUseCase = listAllUseCase;
        this.listMissingUseCase = listMissingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WithholdingCertificateResponse register(@RequestParam Long companyId,
            @Valid @RequestBody RegisterWithholdingCertificateRequest request) {
        return WithholdingCertificateResponse.from(registerUseCase.execute(
                new RegisterWithholdingCertificateCommand(companyId, request.issuedByTaxId(),
                        request.certificateNumber(), request.withholdingType(),
                        request.fiscalYear(), request.fiscalPeriodKey(), request.ratePercent(),
                        request.certifiedAmount(), request.issuedOn(), request.legalDeadlineOn())));
    }

    /** El papel llego: se cierra la expectativa con su fecha y su archivo. */
    @PatchMapping("/{id}/receive")
    public WithholdingCertificateResponse receive(@PathVariable Long id,
            @Valid @RequestBody ReceiveWithholdingCertificateRequest request) {
        return WithholdingCertificateResponse
                .from(receiveUseCase.execute(new ReceiveWithholdingCertificateCommand(id,
                        request.receivedOn(), request.fileRef())));
    }

    /** El cliente no lo expidio: se acredita con el comprobante de pago. */
    @PatchMapping("/{id}/substitute-evidence")
    public WithholdingCertificateResponse attachSubstituteEvidence(@PathVariable Long id,
            @Valid @RequestBody AttachSubstituteEvidenceRequest request) {
        return WithholdingCertificateResponse
                .from(attachSubstituteUseCase.execute(new AttachSubstituteEvidenceCommand(id,
                        request.evidenceKind(), request.evidenceRef())));
    }

    @GetMapping
    public PageResponse<WithholdingCertificateResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(companyId, page, pageSize),
                WithholdingCertificateResponse::from);
    }

    /**
     * Lo que falta por recibir en todas las clinicas antes de que sea tarde. Es el
     * listado por el que existe {@code legal_deadline_on} como columna.
     */
    @GetMapping("/missing")
    public PageResponse<WithholdingCertificateResponse> listMissing(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listMissingUseCase.listMissing(deadlineBefore, page, pageSize),
                WithholdingCertificateResponse::from);
    }
}
