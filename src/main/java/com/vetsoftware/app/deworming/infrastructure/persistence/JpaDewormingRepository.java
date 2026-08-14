package com.vetsoftware.app.deworming.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDewormingRepository implements DewormingRepository {

    private static final int BY_ANIMAL_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_ANIMAL_MAX_PAGE_SIZE = 200;
    private final DewormingJpaRepository jpaRepository;
    private final DewormingJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaDewormingRepository(DewormingJpaRepository jpaRepository, DewormingJpaMapper mapper,
            AnimalJpaRepository animalJpaRepository,
            ConsultationJpaRepository consultationJpaRepository,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Deworming save(Deworming deworming) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(deworming.getAnimal().id());
        ConsultationJpaEntity consultation = deworming.getConsultation() == null
                ? null
                : consultationJpaRepository.getReferenceById(deworming.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(deworming.getCompany().id());
        DewormingJpaEntity saved = jpaRepository
                .save(mapper.toJpa(deworming, animal, consultation, company));
        return mapper.toDomain(saved, deworming.getAnimal(), deworming.getConsultation(),
                deworming.getCompany());
    }

    @Override
    public Optional<Deworming> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Deworming> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Deworming> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Deworming> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId,
            String query, int page, int pageSize) {
        Page<DewormingJpaEntity> result = jpaRepository.findAllByAnimalIdAndCompanyId(animalId,
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
