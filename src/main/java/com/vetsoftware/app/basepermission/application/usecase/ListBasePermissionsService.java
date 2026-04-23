package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.ListBasePermissionsUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "basepermission.list")
@Service
public class ListBasePermissionsService implements ListBasePermissionsUseCase {
    private final BasePermissionRepository repository;

    public ListBasePermissionsService(BasePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BasePermissionDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(BasePermissionDto::from).toList();
    }
}
