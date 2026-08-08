package com.vetsoftware.app.spa.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaEntity;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSpaRepository implements SpaRepository {

    private static final int BY_ANIMAL_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_ANIMAL_MAX_PAGE_SIZE = 200;
    private final SpaJpaRepository jpaRepository;
    private final SpaJpaMapper mapper;
    private final SpaTypeJpaRepository spaTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSpaRepository(SpaJpaRepository jpaRepository, SpaJpaMapper mapper,
            SpaTypeJpaRepository spaTypeJpaRepository, AnimalJpaRepository animalJpaRepository,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.spaTypeJpaRepository = spaTypeJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Spa save(Spa spa) {
        SpaTypeJpaEntity spaType = spaTypeJpaRepository.getReferenceById(spa.getSpaType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(spa.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(spa.getCompany().id());
        SpaJpaEntity saved = jpaRepository.save(mapper.toJpa(spa, spaType, animal, company));
        return mapper.toDomain(saved, spa.getSpaType(), spa.getAnimal(), spa.getCompany());
    }

    @Override
    public Optional<Spa> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Spa> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Spa> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Spa> findAllByAnimalId(Long animalId, int page, int pageSize) {
        Page<SpaJpaEntity> result = jpaRepository.findAllByAnimalId(animalId,
                byAnimalPageRequest(page, pageSize));
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
