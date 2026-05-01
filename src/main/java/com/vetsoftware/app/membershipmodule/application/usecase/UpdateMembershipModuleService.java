package com.vetsoftware.app.membershipmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.command.UpdateMembershipModuleCommand;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;
import com.vetsoftware.app.membershipmodule.application.port.in.UpdateMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipModuleRepository;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipValidationPort;
import com.vetsoftware.app.membershipmodule.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.membershipmodule.domain.MembershipModule;
import com.vetsoftware.app.membershipmodule.domain.MembershipModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membershipmodule.update")
@Service
public class UpdateMembershipModuleService implements UpdateMembershipModuleUseCase {
    private final MembershipModuleRepository repository;
    private final MembershipValidationPort membershipValidationPort;
    private final SubModuleValidationPort subModuleValidationPort;

    public UpdateMembershipModuleService(MembershipModuleRepository repository,
                                          MembershipValidationPort membershipValidationPort,
                                          SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.membershipValidationPort = membershipValidationPort;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    @Transactional
    public MembershipModuleDto execute(UpdateMembershipModuleCommand command, AuthContext auth) {
        MembershipModule membershipModule = repository.findById(command.id())
            .orElseThrow(() -> new MembershipModuleNotFoundException(command.id()));
        membershipValidationPort.validateExists(command.membershipId());
        subModuleValidationPort.validateExists(command.subModuleId());
        membershipModule.update(command.membershipId(), command.subModuleId());
        return MembershipModuleDto.from(repository.save(membershipModule));
    }
}
