package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.ReactivateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.tax.profile.reactivate")
@Service
public class ReactivateCompanyTaxProfileService implements ReactivateCompanyTaxProfileUseCase {
  private final CompanyTaxProfileRepository repository;

  public ReactivateCompanyTaxProfileService(CompanyTaxProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public CompanyTaxProfileDto execute(Long companyId) {
    int rows = repository.reactivate(companyId);
    if (rows == 0) throw new CompanyTaxProfileNotFoundException(companyId);
    return CompanyTaxProfileDto.from(
        repository
            .findByCompanyId(companyId)
            .orElseThrow(() -> new CompanyTaxProfileNotFoundException(companyId)));
  }
}
