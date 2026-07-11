package com.vetsoftware.app.branch.application.usecase;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.in.FindBranchUseCase;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "branch.find")
@Service
public class FindBranchService implements FindBranchUseCase {
    private final BranchRepository repository;

    public FindBranchService(BranchRepository repository) {
        this.repository = repository;
    }

    @Override
    public BranchDto findById(Long id, Long companyId) {
        return BranchDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new BranchNotFoundException(id)));
    }
}
