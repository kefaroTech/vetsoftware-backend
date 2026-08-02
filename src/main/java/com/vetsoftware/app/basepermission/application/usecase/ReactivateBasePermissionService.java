package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.ReactivateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.permission.reactivate")
@Service
public class ReactivateBasePermissionService implements ReactivateBasePermissionUseCase {
    private final BasePermissionRepository repository;

    public ReactivateBasePermissionService(BasePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BasePermissionDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new BasePermissionNotFoundException(id);
        return BasePermissionDto.from(
                repository.findById(id).orElseThrow(() -> new BasePermissionNotFoundException(id)));
    }
}
