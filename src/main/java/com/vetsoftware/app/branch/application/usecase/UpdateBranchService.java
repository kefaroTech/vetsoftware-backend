package com.vetsoftware.app.branch.application.usecase;

import com.vetsoftware.app.branch.application.command.UpdateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.in.UpdateBranchUseCase;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.application.port.out.CityQueryPort;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import com.vetsoftware.app.branch.domain.CityRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "branch.update")
@Service
public class UpdateBranchService implements UpdateBranchUseCase {
    private final BranchRepository repository;
    private final CityQueryPort cityQueryPort;

    public UpdateBranchService(BranchRepository repository, CityQueryPort cityQueryPort) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
    }

    @Override
    @Transactional
    public BranchDto execute(UpdateBranchCommand command) {
        Branch branch = repository.findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new BranchNotFoundException(command.id()));
        String code = command.code() == null ? null : command.code().trim();
        if (code != null && repository.codeExistsForOther(command.companyId(), code, command.id())) {
            throw new IllegalArgumentException("Branch code already in use: " + code);
        }
        CityRef city = cityQueryPort.findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
        branch.update(command.name(), code, command.address(), command.phone(), city);
        return BranchDto.from(repository.save(branch));
    }
}
