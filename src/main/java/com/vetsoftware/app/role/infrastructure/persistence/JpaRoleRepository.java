package com.vetsoftware.app.role.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRoleRepository implements RoleRepository {
    private final RoleJpaRepository jpaRepository;
    private final RoleJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaRoleRepository(RoleJpaRepository jpaRepository,
                             RoleJpaMapper mapper,
                             CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Role save(Role role) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(role.getCompany().id());
        RoleJpaEntity saved = jpaRepository.save(mapper.toJpa(role, company));
        return mapper.toDomain(saved, role.getCompany());
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
