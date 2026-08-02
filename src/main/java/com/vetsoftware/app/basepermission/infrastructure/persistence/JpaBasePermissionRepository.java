package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBasePermissionRepository implements BasePermissionRepository {
    private final BasePermissionJpaRepository jpaRepository;
    private final BasePermissionJpaMapper mapper;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaBasePermissionRepository(BasePermissionJpaRepository jpaRepository,
            BasePermissionJpaMapper mapper, SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public BasePermission save(BasePermission basePermission) {
        SubModuleJpaEntity subModule = subModuleJpaRepository
                .getReferenceById(basePermission.getSubModule().id());
        BasePermissionJpaEntity saved = jpaRepository.save(mapper.toJpa(basePermission, subModule));
        return mapper.toDomain(saved, basePermission.getSubModule());
    }

    @Override
    public Optional<BasePermission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<BasePermission> findAll() {
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
