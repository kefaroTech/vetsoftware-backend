package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.CreateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membership.create")
@Service
public class CreateMembershipService implements CreateMembershipUseCase {
    private final MembershipRepository repository;

    public CreateMembershipService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public MembershipDto execute(CreateMembershipCommand command, AuthContext auth) {
        MembershipStatus status = MembershipStatus.valueOf(command.status().toUpperCase());
        Membership membership = Membership.create(command.name(), status);
        return MembershipDto.from(repository.save(membership));
    }
}
