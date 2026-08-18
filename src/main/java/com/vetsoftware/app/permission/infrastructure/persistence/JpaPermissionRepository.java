package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
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
            PermissionJpaMapper mapper, CompanyJpaRepository companyJpaRepository,
            SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public Permission save(Permission permission) {
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(permission.getCompany().id());
        SubModuleJpaEntity subModule = subModuleJpaRepository
                .getReferenceById(permission.getSubModule().id());
        PermissionJpaEntity saved = jpaRepository
                .save(mapper.toJpa(permission, company, subModule));
        return mapper.toDomain(saved, permission.getCompany(), permission.getSubModule());
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Permission> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Permission> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
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
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
