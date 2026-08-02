package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.registration.application.port.out.CompanyIdentifierChecker;
import org.springframework.stereotype.Component;

@Component
public class JpaCompanyIdentifierChecker implements CompanyIdentifierChecker {
  private final CompanyJpaRepository jpaRepository;

  public JpaCompanyIdentifierChecker(CompanyJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean exists(String identifier) {
    return jpaRepository.existsByIdentifier(identifier);
  }
}
