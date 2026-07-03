package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.Owner;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOwnerRepository implements OwnerRepository {
    private final OwnerJpaRepository jpaRepository;
    private final OwnerJpaMapper mapper;
    private final CityJpaRepository cityJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaOwnerRepository(OwnerJpaRepository jpaRepository,
                              OwnerJpaMapper mapper,
                              CityJpaRepository cityJpaRepository,
                              CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.cityJpaRepository = cityJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Owner save(Owner owner) {
        CityJpaEntity city = cityJpaRepository.getReferenceById(owner.getCity().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(owner.getCompany().id());
        OwnerJpaEntity saved = jpaRepository.save(mapper.toJpa(owner, city, company));
        return mapper.toDomain(saved, owner.getCity(), owner.getCompany());
    }

    @Override
    public Optional<Owner> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Owner> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Owner> searchByCompanyAndTerm(Long companyId, String query) {
        return jpaRepository.searchByCompanyAndTerm(companyId, query)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndCompanyId(id, companyId).ifPresent(jpaRepository::delete);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
