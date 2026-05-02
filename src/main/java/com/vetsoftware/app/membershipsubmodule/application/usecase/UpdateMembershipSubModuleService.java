package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.UpdateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipQueryPort;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membershipsubmodule.update")
@Service
public class UpdateMembershipSubModuleService implements UpdateMembershipSubModuleUseCase {
    private final MembershipSubModuleRepository repository;
    private final MembershipQueryPort membershipQueryPort;
    private final SubModuleQueryPort subModuleQueryPort;

    public UpdateMembershipSubModuleService(MembershipSubModuleRepository repository,
                                             MembershipQueryPort membershipQueryPort,
                                             SubModuleQueryPort subModuleQueryPort) {
        this.repository = repository;
        this.membershipQueryPort = membershipQueryPort;
        this.subModuleQueryPort = subModuleQueryPort;
    }

    @Override
    @Transactional
    public MembershipSubModuleDto execute(UpdateMembershipSubModuleCommand command, AuthContext auth) {
        MembershipSubModule membershipSubModule = repository.findById(command.id())
            .orElseThrow(() -> new MembershipSubModuleNotFoundException(command.id()));
        MembershipRef membership = membershipQueryPort.findById(command.membershipId())
            .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + command.membershipId()));
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
            .orElseThrow(() -> new IllegalArgumentException("SubModule not found: " + command.subModuleId()));
        membershipSubModule.update(membership, subModule);
        return MembershipSubModuleDto.from(repository.save(membershipSubModule));
    }
}
