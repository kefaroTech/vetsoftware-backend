package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.application.dto.PageResult;
import com.vetsoftware.app.owner.domain.Owner;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOwnerRepository implements OwnerRepository {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
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
        return toPageResult(
                jpaRepository.findAllByCompanyId(companyId, pageRequest(page, pageSize)));
    }

    @Override
    public PageResult<Owner> searchByCompanyAndTerm(Long companyId, String query, int page,
            int pageSize) {
        return toPageResult(jpaRepository.searchByCompanyAndTerm(companyId, query,
                pageRequest(page, pageSize)));
    }

    /**
     * Normaliza lo que llega del cliente: una página negativa o un tamaño absurdo
     * no deben poder pedir la tabla entera, que es justo el fallo que se está
     * corrigiendo. El orden es estable porque sin él la paginación no es
     * determinista y una misma fila puede salir en dos páginas.
     */
    private static PageRequest pageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")));
    }

    private PageResult<Owner> toPageResult(Page<OwnerJpaEntity> page) {
        return new PageResult<>(page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
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
