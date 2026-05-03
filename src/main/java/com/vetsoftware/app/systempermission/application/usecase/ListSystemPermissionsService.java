package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.ListSystemPermissionsUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "systempermission.list")
@Service
public class ListSystemPermissionsService implements ListSystemPermissionsUseCase {
    private final SystemPermissionRepository repository;

    public ListSystemPermissionsService(SystemPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SystemPermissionDto> listAll() {
        return repository.findAll().stream().map(SystemPermissionDto::from).toList();
    }
}
