package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyRepository implements CompanyRepository {
    private final CompanyJpaRepository jpaRepository;
    private final CompanyJpaMapper mapper;
    private final CityJpaRepository cityJpaRepository;
    private final MembershipJpaRepository membershipJpaRepository;

    public JpaCompanyRepository(CompanyJpaRepository jpaRepository, CompanyJpaMapper mapper,
            CityJpaRepository cityJpaRepository, MembershipJpaRepository membershipJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.cityJpaRepository = cityJpaRepository;
        this.membershipJpaRepository = membershipJpaRepository;
    }

    @Override
    public Company save(Company company) {
        CityJpaEntity city = cityJpaRepository.getReferenceById(company.getCity().id());
        MembershipJpaEntity membership = membershipJpaRepository
                .getReferenceById(company.getMembership().id());
        CompanyJpaEntity saved = jpaRepository.save(mapper.toJpa(company, city, membership));
        return mapper.toDomain(saved, company.getCity(), company.getMembership());
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

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
