package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RoleQueryPort;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("rolepermissionJpaRoleQueryPort")
public class JpaRoleQueryPort implements RoleQueryPort {
    private final RoleJpaRepository roleJpaRepository;

    public JpaRoleQueryPort(RoleJpaRepository roleJpaRepository) {
        this.roleJpaRepository = roleJpaRepository;
    }

    @Override
    public Optional<RoleRef> findById(Long roleId) {
        return roleJpaRepository.findById(roleId)
            .map(e -> new RoleRef(e.getId(), e.getName(), e.getCode()));
    }
}
