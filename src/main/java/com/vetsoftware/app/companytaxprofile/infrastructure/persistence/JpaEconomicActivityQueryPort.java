package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("companyTaxProfileJpaEconomicActivityQueryPort")
public class JpaEconomicActivityQueryPort implements EconomicActivityQueryPort {
  private final EconomicActivityJpaRepository economicActivityJpaRepository;

  public JpaEconomicActivityQueryPort(EconomicActivityJpaRepository economicActivityJpaRepository) {
    this.economicActivityJpaRepository = economicActivityJpaRepository;
  }

  @Override
  public Optional<EconomicActivityRef> findById(Long economicActivityId) {
    return economicActivityJpaRepository
        .findById(economicActivityId)
        .map(e -> new EconomicActivityRef(e.getId(), e.getCode(), e.getName()));
  }
}
