package com.vetsoftware.app.membershipmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.port.in.DeleteMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipModuleRepository;
import com.vetsoftware.app.membershipmodule.domain.MembershipModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membershipmodule.delete")
@Service
public class DeleteMembershipModuleService implements DeleteMembershipModuleUseCase {
    private final MembershipModuleRepository repository;

    public DeleteMembershipModuleService(MembershipModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new MembershipModuleNotFoundException(id));
        repository.delete(id);
    }
}
