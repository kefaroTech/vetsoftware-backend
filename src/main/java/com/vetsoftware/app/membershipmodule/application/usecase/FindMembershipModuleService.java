package com.vetsoftware.app.membershipmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;
import com.vetsoftware.app.membershipmodule.application.port.in.FindMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipModuleRepository;
import com.vetsoftware.app.membershipmodule.domain.MembershipModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membershipmodule.find")
@Service
public class FindMembershipModuleService implements FindMembershipModuleUseCase {
    private final MembershipModuleRepository repository;

    public FindMembershipModuleService(MembershipModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public MembershipModuleDto findById(Long id, AuthContext auth) {
        return repository.findById(id)
            .map(MembershipModuleDto::from)
            .orElseThrow(() -> new MembershipModuleNotFoundException(id));
    }
}
