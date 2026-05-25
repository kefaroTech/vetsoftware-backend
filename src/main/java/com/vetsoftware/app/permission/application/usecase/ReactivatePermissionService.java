package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.ReactivatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "permission.reactivate")
@Service
public class ReactivatePermissionService implements ReactivatePermissionUseCase {
    private final PermissionRepository repository;

    public ReactivatePermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PermissionDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new PermissionNotFoundException(id);
        return PermissionDto.from(repository.findById(id)
            .orElseThrow(() -> new PermissionNotFoundException(id)));
    }
}
