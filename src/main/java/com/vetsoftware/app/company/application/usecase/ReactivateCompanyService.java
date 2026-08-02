package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ReactivateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.reactivate")
@Service
public class ReactivateCompanyService implements ReactivateCompanyUseCase {
  private final CompanyRepository repository;

  public ReactivateCompanyService(CompanyRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public CompanyDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new CompanyNotFoundException(id);
    return CompanyDto.from(
        repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id)));
  }
}
