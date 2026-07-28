package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.FindMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membership.submodule.find")
@Service
public class FindMembershipSubModuleService implements FindMembershipSubModuleUseCase {
    private final MembershipSubModuleRepository repository;

    public FindMembershipSubModuleService(MembershipSubModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public MembershipSubModuleDto findById(Long id) {
        return repository.findById(id)
            .map(MembershipSubModuleDto::from)
            .orElseThrow(() -> new MembershipSubModuleNotFoundException(id));
    }
}
