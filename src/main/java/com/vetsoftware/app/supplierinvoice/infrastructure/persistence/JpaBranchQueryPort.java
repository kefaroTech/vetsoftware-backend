package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.supplierinvoice.application.port.out.BranchQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("supplierInvoiceJpaBranchQueryPort")
public class JpaBranchQueryPort implements BranchQueryPort {
    private final BranchJpaRepository branchJpaRepository;

    public JpaBranchQueryPort(BranchJpaRepository branchJpaRepository) {
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public Optional<BranchRef> findByIdAndCompanyId(Long branchId, Long companyId) {
        return branchJpaRepository.findByIdAndCompanyId(branchId, companyId)
            .map(e -> new BranchRef(e.getId(), e.getName()));
    }
}
