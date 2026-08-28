package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListMissingWithholdingCertificatesByCompanyUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** El mismo barrido de vencimientos, acotado a la empresa que pregunta. */
@Observed(name = "withholding.certificate.list.missing.by.company")
@Service
public class ListMissingWithholdingCertificatesByCompanyService
        implements
            ListMissingWithholdingCertificatesByCompanyUseCase {

    private final WithholdingCertificateRepository repository;

    public ListMissingWithholdingCertificatesByCompanyService(
            WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<WithholdingCertificateDto> listMissingByCompany(Long companyId,
            LocalDate deadlineBefore, int page, int pageSize) {
        return repository.findAllMissingByCompanyId(companyId, deadlineBefore, page, pageSize)
                .map(WithholdingCertificateDto::from);
    }
}
