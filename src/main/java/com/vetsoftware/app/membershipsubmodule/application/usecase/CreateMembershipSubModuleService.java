package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.CreateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipQueryPort;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public MembershipSubModuleDto execute(CreateMembershipSubModuleCommand command) {
        MembershipRef membership = membershipQueryPort.findById(command.membershipId())
            .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + command.membershipId()));
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
            .orElseThrow(() -> new IllegalArgumentException("SubModule not found: " + command.subModuleId()));

        Optional<Long> disabledId = repository
            .findDisabledIdByMembershipAndSubModule(command.membershipId(), command.subModuleId());
        if (disabledId.isPresent()) {
            Long id = disabledId.get();
            repository.reactivate(id);
            MembershipSubModule refreshed = repository.findById(id)
                .orElseThrow(() -> new MembershipSubModuleNotFoundException(id));
            return MembershipSubModuleDto.from(refreshed);
        }

        MembershipSubModule membershipSubModule = MembershipSubModule.create(membership, subModule);
        return MembershipSubModuleDto.from(repository.save(membershipSubModule));
    }
}
