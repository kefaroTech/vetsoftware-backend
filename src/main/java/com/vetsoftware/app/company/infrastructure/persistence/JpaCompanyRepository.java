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

    /**
     * La rama sin acotar vive aqui y no en el caso de uso a proposito: asi el
     * puerto no ofrece ninguna forma de pedir el registro completo sin declarar el
     * alcance, y un futuro caso de uso no puede tropezar con un {@code findAll()}
     * disponible. Con la empresa informada, «listar empresas» es exactamente una
     * fila —la suya—, que es la unica que el empleado tiene derecho a ver.
     */
    @Override
    public List<Company> findAllVisibleTo(Long companyId) {
        if (companyId == null) {
            return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
        }
        return jpaRepository.findById(companyId).map(mapper::toDomain).map(List::of)
                .orElseGet(List::of);
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
