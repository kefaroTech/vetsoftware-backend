package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.BranchInfoQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter del {@link BranchInfoQueryPort}: cruce permitido de vertical slicing (persistence → persistence de
 * branch) para leer nombre/código/dirección de la sede emisora del POS.
 */
@Component
public class JpaBranchInfoQueryPort implements BranchInfoQueryPort {
    private final BranchJpaRepository branchJpaRepository;

    public JpaBranchInfoQueryPort(BranchJpaRepository branchJpaRepository) {
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public Optional<BranchInfo> findById(Long branchId) {
        if (branchId == null) return Optional.empty();
        return branchJpaRepository.findById(branchId)
                .map(b -> new BranchInfo(b.getName(), b.getCode(), b.getAddress()));
    }
}
