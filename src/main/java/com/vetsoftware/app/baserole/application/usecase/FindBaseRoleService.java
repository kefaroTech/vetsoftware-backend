package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.FindBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "baserole.find")
@Service
public class FindBaseRoleService implements FindBaseRoleUseCase {
    private final BaseRoleRepository repository;

    public FindBaseRoleService(BaseRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public BaseRoleDto findById(Long id, AuthContext auth) {
        return repository.findById(id)
            .map(BaseRoleDto::from)
            .orElseThrow(() -> new BaseRoleNotFoundException(id));
    }
}
