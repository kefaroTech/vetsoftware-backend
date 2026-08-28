package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.withholdingcertificate.application.command.AttachSubstituteEvidenceCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.AttachSubstituteEvidenceUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adjunta el comprobante de pago cuando el cliente no expidio el certificado.
 *
 * <p>
 * Misma forma y mismos motivos que
 * {@link ReceiveWithholdingCertificateService}: {@code @Transactional} porque
 * lee y escribe, y carga ancha porque a este servicio solo llega un principal
 * SYSTEM. La regla de «solo mientras el papel no ha llegado» no esta aqui sino
 * en el agregado, que es donde vive.
 */
@Observed(name = "withholding.certificate.substitute")
@Service
public class AttachSubstituteEvidenceService implements AttachSubstituteEvidenceUseCase {

    private final WithholdingCertificateRepository repository;

    public AttachSubstituteEvidenceService(WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public WithholdingCertificateDto execute(AttachSubstituteEvidenceCommand command) {
        WithholdingCertificate certificate = repository.findById(command.id())
                .orElseThrow(() -> new WithholdingCertificateNotFoundException(command.id()));
        certificate.attachSubstituteEvidence(command.evidenceKind(), command.evidenceRef());
        return WithholdingCertificateDto.from(repository.save(certificate));
    }
}
