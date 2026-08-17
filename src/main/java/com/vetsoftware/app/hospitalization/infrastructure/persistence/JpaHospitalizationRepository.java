package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationRepository implements HospitalizationRepository {

    private final HospitalizationJpaRepository jpaRepository;
    private final HospitalizationJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaHospitalizationRepository(HospitalizationJpaRepository jpaRepository,
            HospitalizationJpaMapper mapper, AnimalJpaRepository animalJpaRepository,
            ConsultationJpaRepository consultationJpaRepository,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Hospitalization save(Hospitalization hospitalization) {
        AnimalJpaEntity animal = animalJpaRepository
                .getReferenceById(hospitalization.getAnimal().id());
        ConsultationJpaEntity consultation = hospitalization.getConsultation() == null
                ? null
                : consultationJpaRepository
                        .getReferenceById(hospitalization.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(hospitalization.getCompany().id());
        HospitalizationJpaEntity saved = jpaRepository
                .save(mapper.toJpa(hospitalization, animal, consultation, company));
        return mapper.toDomain(saved, hospitalization.getAnimal(),
                hospitalization.getConsultation(), hospitalization.getCompany());
    }

    @Override
    public Optional<Hospitalization> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Hospitalization> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Hospitalization> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Hospitalization> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_Id(companyId).stream().map(mapper::toDomain).toList();
    }

    /**
     * El orden por id descendente es estable y devuelve primero lo mas reciente,
     * que es lo que la ficha clinica muestra arriba.
     */
    @Override
    public PageResult<Hospitalization> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId,
            String query, int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByAnimalIdAndCompanyId(animalId, companyId, query,
                        Pages.request(page, pageSize, Sort.by(Sort.Direction.DESC, "id"))),
                mapper::toDomain);
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
