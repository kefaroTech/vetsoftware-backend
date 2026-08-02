package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import org.springframework.stereotype.Component;

@Component("cashRegisterBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {

  private final BranchJpaRepository branchJpaRepository;

  public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
    this.branchJpaRepository = branchJpaRepository;
  }

  @Override
  public boolean existsActiveInCompany(Long branchId, Long companyId) {
    if (branchId == null || companyId == null) return false;
    return branchJpaRepository
        .findByIdAndCompanyId(branchId, companyId)
        .filter(BranchJpaEntity::isActive)
        .isPresent();
  }
}
