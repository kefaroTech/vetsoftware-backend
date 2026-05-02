package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyRepository implements CompanyRepository {
    private final CompanyJpaRepository jpaRepository;
    private final CompanyJpaMapper mapper;
    private final CityJpaRepository cityJpaRepository;

    public JpaCompanyRepository(CompanyJpaRepository jpaRepository,
                                CompanyJpaMapper mapper,
                                CityJpaRepository cityJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.cityJpaRepository = cityJpaRepository;
    }

    @Override
    public Company save(Company company) {
        CityJpaEntity city = cityJpaRepository.getReferenceById(company.getCity().id());
        CompanyJpaEntity saved = jpaRepository.save(mapper.toJpa(company, city));
        return mapper.toDomain(saved, company.getCity());
    }

    @Override
    public Optional<Company> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
