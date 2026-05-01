package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.UpdateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipValidationPort;
import com.vetsoftware.app.membershipsubmodule.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membershipsubmodule.update")
@Service
public class UpdateMembershipSubModuleService implements UpdateMembershipSubModuleUseCase {
    private final MembershipSubModuleRepository repository;
    private final MembershipValidationPort membershipValidationPort;
    private final SubModuleValidationPort subModuleValidationPort;

    public UpdateMembershipSubModuleService(MembershipSubModuleRepository repository,
                                             MembershipValidationPort membershipValidationPort,
                                             SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.membershipValidationPort = membershipValidationPort;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    @Transactional
    public MembershipSubModuleDto execute(UpdateMembershipSubModuleCommand command, AuthContext auth) {
        MembershipSubModule membershipSubModule = repository.findById(command.id())
            .orElseThrow(() -> new MembershipSubModuleNotFoundException(command.id()));
        membershipValidationPort.validateExists(command.membershipId());
        subModuleValidationPort.validateExists(command.subModuleId());
        membershipSubModule.update(command.membershipId(), command.subModuleId());
        return MembershipSubModuleDto.from(repository.save(membershipSubModule));
    }
}
