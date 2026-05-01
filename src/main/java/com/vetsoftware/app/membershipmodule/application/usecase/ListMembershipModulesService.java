package com.vetsoftware.app.membershipmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;
import com.vetsoftware.app.membershipmodule.application.port.in.ListMembershipModulesUseCase;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipModuleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "membershipmodule.list")
@Service
public class ListMembershipModulesService implements ListMembershipModulesUseCase {
    private final MembershipModuleRepository repository;

    public ListMembershipModulesService(MembershipModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MembershipModuleDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(MembershipModuleDto::from).toList();
    }
}
