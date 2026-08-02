package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ReactivateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membership.submodule.reactivate")
@Service
public class ReactivateMembershipSubModuleService implements ReactivateMembershipSubModuleUseCase {
    private final MembershipSubModuleRepository repository;

    public ReactivateMembershipSubModuleService(MembershipSubModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MembershipSubModuleDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new MembershipSubModuleNotFoundException(id);
        return MembershipSubModuleDto.from(repository.findById(id)
                .orElseThrow(() -> new MembershipSubModuleNotFoundException(id)));
    }
}
