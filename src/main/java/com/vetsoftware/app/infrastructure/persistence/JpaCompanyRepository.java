package com.vetsoftware.app.infrastructure.persistence;

import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyRepository implements CompanyRepository {
    private final CompanyJpaRepository jpaRepository;
    private final CompanyJpaMapper mapper;

    public JpaCompanyRepository(CompanyJpaRepository jpaRepository, CompanyJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Company company) {
        jpaRepository.save(mapper.toJpa(company));
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(CompanyId id) {
        jpaRepository.deleteById(id.value());
    }
}
