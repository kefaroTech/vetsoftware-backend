package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.FindPermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "permission.find")
@Service
public class FindPermissionService implements FindPermissionUseCase {
    private final PermissionRepository repository;

    public FindPermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PermissionDto findById(Long id) {
        return repository.findById(id)
            .map(PermissionDto::from)
            .orElseThrow(() -> new PermissionNotFoundException(id));
    }
}
