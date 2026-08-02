package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.employeebranch.application.port.out.BranchQueryPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("employeeBranchJpaBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {

  private final BranchJpaRepository branchJpaRepository;

  public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
    this.branchJpaRepository = branchJpaRepository;
  }

  @Override
  public List<Long> findBranchIdsByCompanyId(Long companyId) {
    return branchJpaRepository.findAllByCompanyId(companyId).stream()
        .map(BranchJpaEntity::getId)
        .toList();
  }
}
