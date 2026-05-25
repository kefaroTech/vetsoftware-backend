package com.vetsoftware.app.systempermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermission;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemPermissionRepository implements SystemPermissionRepository {
    private final SystemPermissionJpaRepository jpaRepository;
    private final SystemPermissionJpaMapper mapper;

    public JpaSystemPermissionRepository(SystemPermissionJpaRepository jpaRepository,
                                         SystemPermissionJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SystemPermission save(SystemPermission systemPermission) {
        SystemPermissionJpaEntity saved = jpaRepository.save(mapper.toJpa(systemPermission));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SystemPermission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SystemPermission> findAll() {
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
}
