package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.CreateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.application.port.out.ModuleValidationPort;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membership.create")
@Service
public class CreateMembershipService implements CreateMembershipUseCase {
    private final MembershipRepository repository;
    private final ModuleValidationPort moduleValidationPort;

    public CreateMembershipService(MembershipRepository repository, ModuleValidationPort moduleValidationPort) {
        this.repository = repository;
        this.moduleValidationPort = moduleValidationPort;
    }

    @Override
    public MembershipDto execute(CreateMembershipCommand command, AuthContext auth) {
        moduleValidationPort.validateAllExist(command.moduleIds());
        MembershipStatus status = MembershipStatus.valueOf(command.status().toUpperCase());
        Membership membership = Membership.create(command.name(), status, command.moduleIds());
        return MembershipDto.from(repository.save(membership));
    }
}
