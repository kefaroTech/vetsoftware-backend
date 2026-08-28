package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.FindWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "withholding.certificate.find")
@Service
public class FindWithholdingCertificateService implements FindWithholdingCertificateUseCase {

    private final WithholdingCertificateRepository repository;

    public FindWithholdingCertificateService(WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    /**
     * <strong>El certificado inexistente y el de otra empresa salen por la misma
     * puerta.</strong> Los dos son un 404 con el mismo mensaje: distinguirlos
     * convertiria el endpoint en un oraculo con el que enumerar los ids ajenos.
     */
    @Override
    public WithholdingCertificateDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(WithholdingCertificateDto::from)
                .orElseThrow(() -> new WithholdingCertificateNotFoundException(id));
    }
}
