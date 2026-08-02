package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.appointment.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("appointmentJpaCompanyQueryPort")
public class JpaCompanyQueryPort implements CompanyQueryPort {
  private final CompanyJpaRepository companyJpaRepository;

  public JpaCompanyQueryPort(CompanyJpaRepository companyJpaRepository) {
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public Optional<String> findNameById(Long companyId) {
    return companyJpaRepository.findById(companyId).map(e -> e.getName());
  }
}
