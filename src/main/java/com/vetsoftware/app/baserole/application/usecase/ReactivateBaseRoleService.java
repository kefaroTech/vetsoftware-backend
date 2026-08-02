package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.ReactivateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.role.reactivate")
@Service
public class ReactivateBaseRoleService implements ReactivateBaseRoleUseCase {
    private final BaseRoleRepository repository;

    public ReactivateBaseRoleService(BaseRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BaseRoleDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new BaseRoleNotFoundException(id);
        return BaseRoleDto
                .from(repository.findById(id).orElseThrow(() -> new BaseRoleNotFoundException(id)));
    }
}
