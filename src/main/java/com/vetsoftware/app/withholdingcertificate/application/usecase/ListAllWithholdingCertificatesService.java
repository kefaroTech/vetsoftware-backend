package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListAllWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El filtro por empresa es opcional porque lo elige la consola de plataforma,
 * no un tenant: el puerto esta cerrado a {@code hasRole('SYSTEM')} y un
 * principal SYSTEM no tiene empresa propia. Con {@code companyId} acota, sin el
 * barre.
 */
@Observed(name = "withholding.certificate.list.all")
@Service
public class ListAllWithholdingCertificatesService
        implements
            ListAllWithholdingCertificatesUseCase {

    private final WithholdingCertificateRepository repository;

    public ListAllWithholdingCertificatesService(WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<WithholdingCertificateDto> listAll(Long companyId, int page, int pageSize) {
        PageResult<WithholdingCertificate> certificates = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return certificates.map(WithholdingCertificateDto::from);
    }
}
