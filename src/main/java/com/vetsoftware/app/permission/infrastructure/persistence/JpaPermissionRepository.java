package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPermissionRepository implements PermissionRepository {
    private final PermissionJpaRepository jpaRepository;
    private final PermissionJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaPermissionRepository(PermissionJpaRepository jpaRepository,
                                    PermissionJpaMapper mapper,
                                    CompanyJpaRepository companyJpaRepository,
                                    SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public Permission save(Permission permission) {
        var company = companyJpaRepository.getReferenceById(permission.getCompanyId());
        var subModule = subModuleJpaRepository.getReferenceById(permission.getSubModuleId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(permission, company, subModule)));
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
