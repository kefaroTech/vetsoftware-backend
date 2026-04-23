package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.port.in.DeleteBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "basepermission.delete")
@Service
public class DeleteBasePermissionService implements DeleteBasePermissionUseCase {
    private final BasePermissionRepository repository;

    public DeleteBasePermissionService(BasePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new BasePermissionNotFoundException(id));
        repository.delete(id);
    }
}
