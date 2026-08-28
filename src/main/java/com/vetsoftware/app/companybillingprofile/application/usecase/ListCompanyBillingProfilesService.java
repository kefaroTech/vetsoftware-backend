package com.vetsoftware.app.companybillingprofile.application.usecase;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.in.ListCompanyBillingProfilesUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.billing.profile.list")
@Service
public class ListCompanyBillingProfilesService implements ListCompanyBillingProfilesUseCase {

    private final CompanyBillingProfileRepository repository;

    public ListCompanyBillingProfilesService(CompanyBillingProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales son los de la consulta y no se recalculan sobre el contenido ya
     * paginado: {@code PageResult.map} conserva los metadatos intactos.
     */
    @Override
    public PageResult<CompanyBillingProfileDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(CompanyBillingProfileDto::from);
    }
}
