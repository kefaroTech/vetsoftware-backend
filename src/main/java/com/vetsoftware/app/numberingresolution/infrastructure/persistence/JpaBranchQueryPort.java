package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.numberingresolution.application.port.out.BranchQueryPort;
import org.springframework.stereotype.Component;

/**
 * Adapter del {@link BranchQueryPort}: cruce permitido de vertical slicing
 * (persistence → persistence de branch) para validar que la sede de una
 * resolución pertenezca a la empresa.
 */
@Component("numberingResolutionJpaBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {
    private final BranchJpaRepository branchJpaRepository;

    public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long branchId, Long companyId) {
        return branchJpaRepository.existsByIdAndCompany_Id(branchId, companyId);
    }
}
