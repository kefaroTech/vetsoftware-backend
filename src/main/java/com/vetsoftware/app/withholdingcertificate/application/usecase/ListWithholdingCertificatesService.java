package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "withholding.certificate.list.by.company")
@Service
public class ListWithholdingCertificatesService implements ListWithholdingCertificatesUseCase {

    private final WithholdingCertificateRepository repository;

    public ListWithholdingCertificatesService(WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<WithholdingCertificateDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(WithholdingCertificateDto::from);
    }
}
