package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.FindSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systempermission.find")
@Service
public class FindSystemPermissionService implements FindSystemPermissionUseCase {
    private final SystemPermissionRepository repository;

    public FindSystemPermissionService(SystemPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public SystemPermissionDto findById(Long id) {
        return repository.findById(id)
            .map(SystemPermissionDto::from)
            .orElseThrow(() -> new SystemPermissionNotFoundException(id));
    }
}
