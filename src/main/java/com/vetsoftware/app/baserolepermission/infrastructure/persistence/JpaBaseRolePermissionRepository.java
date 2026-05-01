package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaRepository;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBaseRolePermissionRepository implements BaseRolePermissionRepository {
    private final BaseRolePermissionJpaRepository jpaRepository;
    private final BaseRolePermissionJpaMapper mapper;
    private final BaseRoleJpaRepository baseRoleJpaRepository;
    private final BasePermissionJpaRepository basePermissionJpaRepository;

    public JpaBaseRolePermissionRepository(BaseRolePermissionJpaRepository jpaRepository,
                                            BaseRolePermissionJpaMapper mapper,
                                            BaseRoleJpaRepository baseRoleJpaRepository,
                                            BasePermissionJpaRepository basePermissionJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.baseRoleJpaRepository = baseRoleJpaRepository;
        this.basePermissionJpaRepository = basePermissionJpaRepository;
    }

    @Override
    public BaseRolePermission save(BaseRolePermission baseRolePermission) {
        var baseRole = baseRoleJpaRepository.getReferenceById(baseRolePermission.getBaseRoleId());
        var basePermission = basePermissionJpaRepository.getReferenceById(baseRolePermission.getBasePermissionId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(baseRolePermission, baseRole, basePermission)));
    }

    @Override
    public Optional<BaseRolePermission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<BaseRolePermission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
