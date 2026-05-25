package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaRepository;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemUserPermissionRepository implements SystemUserPermissionRepository {
    private final SystemUserPermissionJpaRepository jpaRepository;
    private final SystemUserPermissionJpaMapper mapper;
    private final SystemUserJpaRepository systemUserJpaRepository;
    private final SystemPermissionJpaRepository systemPermissionJpaRepository;

    public JpaSystemUserPermissionRepository(SystemUserPermissionJpaRepository jpaRepository,
                                             SystemUserPermissionJpaMapper mapper,
                                             SystemUserJpaRepository systemUserJpaRepository,
                                             SystemPermissionJpaRepository systemPermissionJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.systemUserJpaRepository = systemUserJpaRepository;
        this.systemPermissionJpaRepository = systemPermissionJpaRepository;
    }

    @Override
    public SystemUserPermission save(SystemUserPermission systemUserPermission) {
        SystemUserJpaEntity systemUser = systemUserJpaRepository.getReferenceById(systemUserPermission.getSystemUser().id());
        SystemPermissionJpaEntity systemPermission = systemPermissionJpaRepository.getReferenceById(systemUserPermission.getSystemPermission().id());
        SystemUserPermissionJpaEntity saved = jpaRepository.save(mapper.toJpa(systemUserPermission, systemUser, systemPermission));
        return mapper.toDomain(saved, systemUserPermission.getSystemUser(), systemUserPermission.getSystemPermission());
    }

    @Override
    public Optional<SystemUserPermission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SystemUserPermission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public Optional<Long> findDisabledIdBySystemUserAndSystemPermission(Long systemUserId, Long systemPermissionId) {
        return jpaRepository.findDisabledIdBySystemUserAndSystemPermission(systemUserId, systemPermissionId);
    }
}
