package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.BranchQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("laboratoryTestJpaBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {

  private static final String PRINCIPAL_CODE = "PRINCIPAL";

  private final BranchJpaRepository branchJpaRepository;

  public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
    this.branchJpaRepository = branchJpaRepository;
  }

  @Override
  public Optional<Long> findActiveIdByIdAndCompanyId(Long branchId, Long companyId) {
    return branchJpaRepository
        .findByIdAndCompanyId(branchId, companyId)
        .filter(BranchJpaEntity::isActive)
        .map(BranchJpaEntity::getId);
  }

  @Override
  public Optional<Long> findDefaultActiveIdByCompanyId(Long companyId) {
    return branchJpaRepository
        .findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(companyId, PRINCIPAL_CODE)
        .or(() -> branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(companyId))
        .map(BranchJpaEntity::getId);
  }
}
