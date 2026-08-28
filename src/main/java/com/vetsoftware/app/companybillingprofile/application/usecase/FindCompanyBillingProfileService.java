package com.vetsoftware.app.companybillingprofile.application.usecase;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.in.FindCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Una ficha del historico por su id, siempre acotada por empresa.
 *
 * <p>
 * <strong>La ficha de otra empresa se comporta como inexistente.</strong> No
 * hay una rama que conteste 403: distinguir «no existe» de «no es tuya» le
 * diria a quien prueba ids si el numero 1 esta ocupado, y para el cliente
 * legitimo las dos respuestas piden lo mismo.
 */
@Observed(name = "company.billing.profile.find")
@Service
public class FindCompanyBillingProfileService implements FindCompanyBillingProfileUseCase {

    private final CompanyBillingProfileRepository repository;

    public FindCompanyBillingProfileService(CompanyBillingProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanyBillingProfileDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(CompanyBillingProfileDto::from)
                .orElseThrow(() -> new CompanyBillingProfileNotFoundException(id));
    }
}
