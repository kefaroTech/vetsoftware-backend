package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.openaccount.application.port.out.BranchQueryPort;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("openAccountJpaBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {

    private static final String PRINCIPAL_CODE = "PRINCIPAL";

    private final BranchJpaRepository branchJpaRepository;

    public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public Optional<BranchRef> findActiveByIdAndCompanyId(Long branchId, Long companyId) {
        return branchJpaRepository.findByIdAndCompanyId(branchId, companyId)
                .filter(BranchJpaEntity::isActive).map(JpaBranchQueryPort::toRef);
    }

    @Override
    public boolean existsByIdAndCompanyId(Long branchId, Long companyId) {
        return branchJpaRepository.existsByIdAndCompany_Id(branchId, companyId);
    }

    @Override
    public Optional<BranchRef> findDefaultActiveByCompanyId(Long companyId) {
        return branchJpaRepository
                .findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(companyId, PRINCIPAL_CODE)
                .or(() -> branchJpaRepository
                        .findFirstByCompany_IdAndActiveTrueOrderByIdAsc(companyId))
                .map(JpaBranchQueryPort::toRef);
    }

    private static BranchRef toRef(BranchJpaEntity e) {
        return new BranchRef(e.getId(), e.getName(), e.getCode());
    }
}
