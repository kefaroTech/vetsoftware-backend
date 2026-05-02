package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.CreateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipQueryPort;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membershipsubmodule.create")
@Service
public class CreateMembershipSubModuleService implements CreateMembershipSubModuleUseCase {
    private final MembershipSubModuleRepository repository;
    private final MembershipQueryPort membershipQueryPort;
    private final SubModuleQueryPort subModuleQueryPort;

    public CreateMembershipSubModuleService(MembershipSubModuleRepository repository,
                                             MembershipQueryPort membershipQueryPort,
                                             SubModuleQueryPort subModuleQueryPort) {
        this.repository = repository;
        this.membershipQueryPort = membershipQueryPort;
        this.subModuleQueryPort = subModuleQueryPort;
    }

    @Override
    public MembershipSubModuleDto execute(CreateMembershipSubModuleCommand command, AuthContext auth) {
        MembershipRef membership = membershipQueryPort.findById(command.membershipId())
            .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + command.membershipId()));
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
            .orElseThrow(() -> new IllegalArgumentException("SubModule not found: " + command.subModuleId()));
        MembershipSubModule membershipSubModule = MembershipSubModule.create(membership, subModule);
        return MembershipSubModuleDto.from(repository.save(membershipSubModule));
    }
}
