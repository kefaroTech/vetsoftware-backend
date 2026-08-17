package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.owner.domain.Owner;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOwnerRepository implements OwnerRepository {

    /**
     * Orden de los dos listados: por nombre, que es como se lee la agenda de
     * propietarios, con el id de desempate. Sin un orden total la paginación no es
     * determinista y una misma fila puede salir en dos páginas.
     */
    private static final Sort BY_NAME_THEN_ID = Sort.by(Sort.Direction.ASC, "name")
            .and(Sort.by(Sort.Direction.ASC, "id"));

    private final OwnerJpaRepository jpaRepository;
    private final OwnerJpaMapper mapper;
    private final CityJpaRepository cityJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaOwnerRepository(OwnerJpaRepository jpaRepository, OwnerJpaMapper mapper,
            CityJpaRepository cityJpaRepository, CompanyJpaRepository companyJpaRepository) {
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
    public PageResult<Owner> findAllByCompanyId(Long companyId, int page, int pageSize) {
        Page<OwnerJpaEntity> result = jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, BY_NAME_THEN_ID));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public PageResult<Owner> searchByCompanyAndTerm(Long companyId, String query, int page,
            int pageSize) {
        Page<OwnerJpaEntity> result = jpaRepository.searchByCompanyAndTerm(companyId, query,
                Pages.request(page, pageSize, BY_NAME_THEN_ID));
        return Pages.result(result, mapper::toDomain);
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
