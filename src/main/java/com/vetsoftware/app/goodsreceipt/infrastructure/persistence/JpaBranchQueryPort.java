package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.BranchQueryPort;
import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("goodsReceiptJpaBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {
    private final BranchJpaRepository branchJpaRepository;

    public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public Optional<BranchRef> findById(Long branchId, Long companyId) {
        return branchJpaRepository.findByIdAndCompanyId(branchId, companyId)
            .map(e -> new BranchRef(e.getId(), e.getName()));
    }
}
