package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionQueryPort;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("rolepermissionJpaPermissionQueryPort")
public class JpaPermissionQueryPort implements PermissionQueryPort {
    private final PermissionJpaRepository permissionJpaRepository;

    public JpaPermissionQueryPort(PermissionJpaRepository permissionJpaRepository) {
        this.permissionJpaRepository = permissionJpaRepository;
    }

    @Override
    public Optional<PermissionRef> findById(Long permissionId) {
        return permissionJpaRepository.findById(permissionId)
            .map(e -> new PermissionRef(e.getId(), e.getName(), e.getCode()));
    }

    @Override
    public Optional<PermissionRef> findByIdAndCompanyId(Long permissionId, Long companyId) {
        return permissionJpaRepository.findByIdAndCompany_Id(permissionId, companyId)
            .map(e -> new PermissionRef(e.getId(), e.getName(), e.getCode()));
    }
}
