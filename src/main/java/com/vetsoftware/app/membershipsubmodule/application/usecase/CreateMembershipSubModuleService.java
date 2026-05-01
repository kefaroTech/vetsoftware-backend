package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.CreateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipValidationPort;
import com.vetsoftware.app.membershipsubmodule.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membershipsubmodule.create")
@Service
public class CreateMembershipSubModuleService implements CreateMembershipSubModuleUseCase {
    private final MembershipSubModuleRepository repository;
    private final MembershipValidationPort membershipValidationPort;
    private final SubModuleValidationPort subModuleValidationPort;

    public CreateMembershipSubModuleService(MembershipSubModuleRepository repository,
                                             MembershipValidationPort membershipValidationPort,
                                             SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.membershipValidationPort = membershipValidationPort;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    public MembershipSubModuleDto execute(CreateMembershipSubModuleCommand command, AuthContext auth) {
        membershipValidationPort.validateExists(command.membershipId());
        subModuleValidationPort.validateExists(command.subModuleId());
        MembershipSubModule membershipSubModule = MembershipSubModule.create(command.membershipId(), command.subModuleId());
        return MembershipSubModuleDto.from(repository.save(membershipSubModule));
    }
}
