package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.command.LinkWithholdingCertificateCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.LinkWithholdingCertificateUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.application.port.out.WithholdingCertificateValidationPort;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholdingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apunta una retencion a su certificado, que es lo que la vuelve descontable.
 *
 * <p>
 * <strong>La carga va acotada por empresa aunque el puerto este cerrado a
 * plataforma.</strong> El {@code @PreAuthorize} decide <em>quien</em> puede
 * llamar; el {@code companyId} del command decide <em>sobre que fila</em>
 * escribe. Sin el segundo, un id equivocado de tesoreria certificaria la
 * retencion de otra clinica, y esa fila ya no se puede corregir editandola: la
 * tabla solo se agrega.
 *
 * <p>
 * <strong>El certificado se comprueba antes de tocar nada.</strong> La FK
 * {@code (company_id, certificate_id)} es compuesta, asi que un certificado de
 * otra empresa lo pararia el motor —pero como error de integridad al hacer
 * flush, es decir, como un 500 sin explicacion para quien lo pidio—.
 */
@Observed(name = "document.withholding.link.certificate")
@Service
public class LinkWithholdingCertificateService implements LinkWithholdingCertificateUseCase {

    private final DocumentWithholdingRepository repository;
    private final WithholdingCertificateValidationPort certificateValidationPort;

    public LinkWithholdingCertificateService(DocumentWithholdingRepository repository,
            WithholdingCertificateValidationPort certificateValidationPort) {
        this.repository = repository;
        this.certificateValidationPort = certificateValidationPort;
    }

    @Override
    @Transactional
    public DocumentWithholdingDto execute(LinkWithholdingCertificateCommand command) {
        DocumentWithholding withholding = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DocumentWithholdingNotFoundException(command.id()));

        if (!certificateValidationPort.existsByIdAndCompanyId(command.certificateId(),
                command.companyId()))
            throw new IllegalArgumentException(
                    "Withholding certificate not found: " + command.certificateId());

        // El repunte a otro certificado lo rechaza el dominio, no este metodo: es
        // una invariante de la retencion y no un paso del caso de uso.
        return DocumentWithholdingDto
                .from(repository.save(withholding.linkTo(command.certificateId())));
    }
}
