package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionQueryPort;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("baserolepermissionJpaBasePermissionQueryPort")
public class JpaBasePermissionQueryPort implements BasePermissionQueryPort {
    private final BasePermissionJpaRepository basePermissionJpaRepository;

    public JpaBasePermissionQueryPort(BasePermissionJpaRepository basePermissionJpaRepository) {
        this.basePermissionJpaRepository = basePermissionJpaRepository;
    }

    @Override
    public Optional<BasePermissionRef> findById(Long basePermissionId) {
        return basePermissionJpaRepository.findById(basePermissionId)
                .map(e -> new BasePermissionRef(e.getId(), e.getName(), e.getCode()));
    }
}
