package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ListMembershipSubModulesUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "membershipsubmodule.list")
@Service
public class ListMembershipSubModulesService implements ListMembershipSubModulesUseCase {
    private final MembershipSubModuleRepository repository;

    public ListMembershipSubModulesService(MembershipSubModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MembershipSubModuleDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(MembershipSubModuleDto::from).toList();
    }
}
