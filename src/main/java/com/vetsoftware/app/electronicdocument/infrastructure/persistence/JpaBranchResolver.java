package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.BranchResolverPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaBranchResolver implements BranchResolverPort {

    private static final String PRINCIPAL_CODE = "PRINCIPAL";

    private final BranchJpaRepository branchJpaRepository;

    public JpaBranchResolver(BranchJpaRepository branchJpaRepository) {
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public Optional<Long> resolve(Long companyId, Long requestedBranchId) {
        // Una venta POS no se emite desde una sede fuera de operación: toda rama exige
        // ACTIVA. Vacío ⇒
        // el
        // builder lanza (sede pedida inactiva/ajena, o la empresa sin ninguna sede
        // activa).
        if (requestedBranchId != null) {
            return branchJpaRepository.findByIdAndCompanyId(requestedBranchId, companyId)
                    .filter(BranchJpaEntity::isActive).map(BranchJpaEntity::getId);
        }
        return branchJpaRepository
                .findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(companyId, PRINCIPAL_CODE)
                .or(() -> branchJpaRepository
                        .findFirstByCompany_IdAndActiveTrueOrderByIdAsc(companyId))
                .map(BranchJpaEntity::getId);
    }
}
