package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRolePermissionRepository implements RolePermissionRepository {
    private final RolePermissionJpaRepository jpaRepository;
    private final RolePermissionJpaMapper mapper;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    public JpaRolePermissionRepository(RolePermissionJpaRepository jpaRepository,
                                       RolePermissionJpaMapper mapper,
                                       RoleJpaRepository roleJpaRepository,
                                       PermissionJpaRepository permissionJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.roleJpaRepository = roleJpaRepository;
        this.permissionJpaRepository = permissionJpaRepository;
    }

    @Override
    public RolePermission save(RolePermission rolePermission) {
        RoleJpaEntity role = roleJpaRepository.getReferenceById(rolePermission.getRole().id());
        PermissionJpaEntity permission = permissionJpaRepository.getReferenceById(rolePermission.getPermission().id());
        RolePermissionJpaEntity saved = jpaRepository.save(mapper.toJpa(rolePermission, role, permission));
        return mapper.toDomain(saved, rolePermission.getRole(), rolePermission.getPermission());
    }

    @Override
    public Optional<RolePermission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RolePermission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
