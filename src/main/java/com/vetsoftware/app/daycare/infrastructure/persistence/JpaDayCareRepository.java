package com.vetsoftware.app.daycare.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDayCareRepository implements DayCareRepository {

    private static final int BY_ANIMAL_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_ANIMAL_MAX_PAGE_SIZE = 200;
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
        Page<DayCareJpaEntity> result = jpaRepository.findAllByAnimalIdAndCompanyId(animalId,
                companyId, query, byAnimalPageRequest(page, pageSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente: una pagina negativa o un tamano desmedido
     * no deben poder volver a pedir el historial entero del animal. El orden por id
     * descendente es estable y devuelve primero lo mas reciente, que es lo que la
     * ficha clinica muestra arriba.
     */
    private static PageRequest byAnimalPageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0
                ? BY_ANIMAL_DEFAULT_PAGE_SIZE
                : Math.min(pageSize, BY_ANIMAL_MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
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
