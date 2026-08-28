package com.vetsoftware.app.companybillingprofile.application.usecase;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.in.FindCurrentCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.billing.profile.find.current")
@Service
public class FindCurrentCompanyBillingProfileService
        implements
            FindCurrentCompanyBillingProfileUseCase {

    private final CompanyBillingProfileRepository repository;

    public FindCurrentCompanyBillingProfileService(CompanyBillingProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Una empresa sin ficha contesta 404, no una ficha vacia: no tener a quien
     * facturar es la ausencia del recurso, y el front tiene que poder distinguirlo
     * de una ficha con campos en blanco para ofrecer «abrir ficha».
     */
    @Override
    public CompanyBillingProfileDto findCurrent(Long companyId) {
        return repository.findCurrentByCompanyId(companyId).map(CompanyBillingProfileDto::from)
                .orElseThrow(() -> CompanyBillingProfileNotFoundException
                        .withoutCurrentProfile(companyId));
    }
}
