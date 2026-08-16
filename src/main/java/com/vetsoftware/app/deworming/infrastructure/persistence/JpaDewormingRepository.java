package com.vetsoftware.app.deworming.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDewormingRepository implements DewormingRepository {

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

    /**
     * El orden por id descendente es estable y devuelve primero lo mas reciente,
     * que es lo que la ficha clinica muestra arriba.
     */
    @Override
    public PageResult<Deworming> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId,
            String query, int page, int pageSize) {
        Page<DewormingJpaEntity> result = jpaRepository.findAllByAnimalIdAndCompanyId(animalId,
                companyId, query,
                Pages.request(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
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
