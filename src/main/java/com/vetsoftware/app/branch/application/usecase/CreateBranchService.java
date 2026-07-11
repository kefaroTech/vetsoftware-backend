package com.vetsoftware.app.branch.application.usecase;

import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.in.CreateBranchUseCase;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.application.port.out.CityQueryPort;
import com.vetsoftware.app.branch.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "branch.create")
@Service
public class CreateBranchService implements CreateBranchUseCase {
    private final BranchRepository repository;
    private final CityQueryPort cityQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateBranchService(BranchRepository repository,
                               CityQueryPort cityQueryPort,
                               CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public BranchDto execute(CreateBranchCommand command) {
        String code = command.code() == null ? null : command.code().trim();
        if (code != null && repository.codeExists(command.companyId(), code)) {
            throw new IllegalArgumentException("Branch code already in use: " + code);
        }
        CityRef city = cityQueryPort.findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        Branch branch = Branch.create(command.name(), code, command.address(), command.phone(), city, company);
        return BranchDto.from(repository.save(branch));
    }
}
