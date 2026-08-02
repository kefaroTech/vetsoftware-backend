package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.FindCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.tax.profile.find")
@Service
public class FindCompanyTaxProfileService implements FindCompanyTaxProfileUseCase {
  private final CompanyTaxProfileRepository repository;

  public FindCompanyTaxProfileService(CompanyTaxProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  public CompanyTaxProfileDto findByCompanyId(Long companyId) {
    return CompanyTaxProfileDto.from(
        repository
            .findByCompanyId(companyId)
            .orElseThrow(() -> new CompanyTaxProfileNotFoundException(companyId)));
  }
}
