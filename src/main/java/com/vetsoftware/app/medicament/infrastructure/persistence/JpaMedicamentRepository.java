package com.vetsoftware.app.medicament.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.medicament.application.dto.PageResult;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMedicamentRepository implements MedicamentRepository {
    private static final int LIST_DEFAULT_PAGE_SIZE = 20;
    private static final int LIST_MAX_PAGE_SIZE = 200;

    private final MedicamentJpaRepository jpaRepository;
    private final MedicamentJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaMedicamentRepository(MedicamentJpaRepository jpaRepository,
            MedicamentJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Medicament save(Medicament medicament) {
        CompanyJpaEntity company = medicament.getCompany() == null
                ? null
                : companyJpaRepository.getReferenceById(medicament.getCompany().id());
        MedicamentJpaEntity saved = jpaRepository.save(mapper.toJpa(medicament, company));
        return mapper.toDomain(saved, medicament.getCompany());
    }

    @Override
    public Optional<Medicament> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Medicament> findAll(int page, int pageSize) {
        Page<MedicamentJpaEntity> result = jpaRepository.findAll(listPageRequest(page, pageSize));
        List<Medicament> content = result.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente y acota el tamano; sin tope se vuelve a
     * pedir la tabla entera por query param. Orden por nombre, que es como se lee
     * un catalogo, con el id de desempate para que la paginacion sea determinista.
     */
    private static PageRequest listPageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0
                ? LIST_DEFAULT_PAGE_SIZE
                : Math.min(pageSize, LIST_MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")));
    }

    @Override
    public List<Medicament> findAllAvailableForCompany(Long companyId) {
        return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Medicament> findAllDisabledForCompany(Long companyId) {
        return jpaRepository.findAllDisabledForCompany(companyId).stream().map(mapper::toDomain)
                .toList();
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
