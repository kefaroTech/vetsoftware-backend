package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.UpdateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.application.port.out.ModuleValidationPort;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membership.update")
@Service
public class UpdateMembershipService implements UpdateMembershipUseCase {
    private final MembershipRepository repository;
    private final ModuleValidationPort moduleValidationPort;

    public UpdateMembershipService(MembershipRepository repository, ModuleValidationPort moduleValidationPort) {
        this.repository = repository;
        this.moduleValidationPort = moduleValidationPort;
    }

    @Override
    @Transactional
    public MembershipDto execute(UpdateMembershipCommand command, AuthContext auth) {
        moduleValidationPort.validateAllExist(command.moduleIds());
        Membership membership = repository.findById(command.id())
            .orElseThrow(() -> new MembershipNotFoundException(command.id()));
        MembershipStatus status = MembershipStatus.valueOf(command.status().toUpperCase());
        membership.update(command.name(), status, command.moduleIds());
        return MembershipDto.from(repository.save(membership));
    }
}
