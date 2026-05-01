package com.vetsoftware.app.membershipmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.command.CreateMembershipModuleCommand;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;
import com.vetsoftware.app.membershipmodule.application.port.in.CreateMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipModuleRepository;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipValidationPort;
import com.vetsoftware.app.membershipmodule.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.membershipmodule.domain.MembershipModule;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membershipmodule.create")
@Service
public class CreateMembershipModuleService implements CreateMembershipModuleUseCase {
    private final MembershipModuleRepository repository;
    private final MembershipValidationPort membershipValidationPort;
    private final SubModuleValidationPort subModuleValidationPort;

    public CreateMembershipModuleService(MembershipModuleRepository repository,
                                         MembershipValidationPort membershipValidationPort,
                                         SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.membershipValidationPort = membershipValidationPort;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    public MembershipModuleDto execute(CreateMembershipModuleCommand command, AuthContext auth) {
        membershipValidationPort.validateExists(command.membershipId());
        subModuleValidationPort.validateExists(command.subModuleId());
        MembershipModule membershipModule = MembershipModule.create(command.membershipId(), command.subModuleId());
        return MembershipModuleDto.from(repository.save(membershipModule));
    }
}
