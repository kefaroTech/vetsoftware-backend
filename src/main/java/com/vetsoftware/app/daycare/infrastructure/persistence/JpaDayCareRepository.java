package com.vetsoftware.app.daycare.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDayCareRepository implements DayCareRepository {

    private final DayCareJpaRepository jpaRepository;
    private final DayCareJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaDayCareRepository(DayCareJpaRepository jpaRepository, DayCareJpaMapper mapper,
            AnimalJpaRepository animalJpaRepository, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public DayCare save(DayCare dayCare) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(dayCare.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(dayCare.getCompany().id());
        DayCareJpaEntity saved = jpaRepository.save(mapper.toJpa(dayCare, animal, company));
        return mapper.toDomain(saved, dayCare.getAnimal(), dayCare.getCompany());
    }

    @Override
    public Optional<DayCare> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DayCare> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<DayCare> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<DayCare> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId,
            String query, int page, int pageSize) {
        // El orden por id descendente es estable y devuelve primero lo mas reciente,
        // que es lo que la ficha clinica muestra arriba.
        Sort order = Sort.by(Sort.Direction.DESC, "id");
        Page<DayCareJpaEntity> result = jpaRepository.findAllByAnimalIdAndCompanyId(animalId,
                companyId, query, Pages.request(page, pageSize, order));
        return Pages.result(result, mapper::toDomain);
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
