package com.vetsoftware.app.branch.application.usecase;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.in.ActivateBranchUseCase;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "branch.activate")
@Service
public class ActivateBranchService implements ActivateBranchUseCase {
    private final BranchRepository repository;

    public ActivateBranchService(BranchRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BranchDto execute(Long id, Long companyId) {
        Branch branch = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BranchNotFoundException(id));
        branch.activate();
        return BranchDto.from(repository.save(branch));
    }
}
