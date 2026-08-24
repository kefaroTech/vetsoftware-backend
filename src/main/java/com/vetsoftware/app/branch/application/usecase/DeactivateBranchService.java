package com.vetsoftware.app.branch.application.usecase;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.in.DeactivateBranchUseCase;
import com.vetsoftware.app.branch.application.port.out.BranchCapacityPort;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "branch.deactivate")
@Service
public class DeactivateBranchService implements DeactivateBranchUseCase {
    private final BranchRepository repository;
    private final BranchCapacityPort branchCapacityPort;

    public DeactivateBranchService(BranchRepository repository,
            BranchCapacityPort branchCapacityPort) {
        this.repository = repository;
        this.branchCapacityPort = branchCapacityPort;
    }

    @Override
    @Transactional
    public BranchDto execute(Long id, Long companyId) {
        Branch branch = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BranchNotFoundException(id));
        // No se puede desactivar la ÚLTIMA sede activa: dejaría a la empresa sin sede
        // operativa y
        // bloquearía
        // las escrituras scopeadas (citas/cuentas/POS resuelven "sede activa por
        // defecto"). Solo aplica
        // si la
        // sede está activa: desactivar una ya inactiva es idempotente y no reduce el
        // número de sedes
        // activas.
        if (branch.isActive() && !repository.existsOtherActiveByCompanyId(companyId, id)) {
            throw new IllegalStateException(
                    "No se puede desactivar la última sucursal activa de la empresa");
        }
        if (!branch.isActive()) {
            return BranchDto.from(branch);
        }
        branch.deactivate();
        Branch saved = repository.save(branch);
        branchCapacityPort.release(companyId);
        return BranchDto.from(saved);
    }
}
