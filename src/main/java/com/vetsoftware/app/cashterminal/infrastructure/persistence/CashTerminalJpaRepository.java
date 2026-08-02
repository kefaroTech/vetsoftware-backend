package com.vetsoftware.app.cashterminal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashTerminalJpaRepository extends JpaRepository<CashTerminalJpaEntity, Long> {
  List<CashTerminalJpaEntity> findAllByCompanyIdAndBranchIdOrderByActiveDescNameAsc(
      Long companyId, Long branchId);

  List<CashTerminalJpaEntity> findAllByCompanyIdAndBranchIdAndActiveTrueOrderByNameAsc(
      Long companyId, Long branchId);

  Optional<CashTerminalJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

  Optional<CashTerminalJpaEntity> findByIdAndCompanyIdAndBranchIdAndActiveTrue(
      Long id, Long companyId, Long branchId);

  boolean existsByCompanyIdAndBranchIdAndCodeIgnoreCase(Long companyId, Long branchId, String code);

  boolean existsByCompanyIdAndBranchIdAndCodeIgnoreCaseAndIdNot(
      Long companyId, Long branchId, String code, Long id);
}
