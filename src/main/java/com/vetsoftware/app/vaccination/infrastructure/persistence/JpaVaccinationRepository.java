package com.vetsoftware.app.vaccination.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVaccinationRepository implements VaccinationRepository {
    private final VaccinationJpaRepository jpaRepository;
    private final VaccinationJpaMapper mapper;
    private final VaccinationTypeJpaRepository vaccinationTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaVaccinationRepository(VaccinationJpaRepository jpaRepository,
                                    VaccinationJpaMapper mapper,
                                    VaccinationTypeJpaRepository vaccinationTypeJpaRepository,
                                    AnimalJpaRepository animalJpaRepository,
                                    ConsultationJpaRepository consultationJpaRepository,
                                    CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.vaccinationTypeJpaRepository = vaccinationTypeJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Vaccination save(Vaccination vaccination) {
        VaccinationTypeJpaEntity vaccinationType =
            vaccinationTypeJpaRepository.getReferenceById(vaccination.getVaccinationType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(vaccination.getAnimal().id());
        ConsultationJpaEntity consultation = vaccination.getConsultation() == null ? null
            : consultationJpaRepository.getReferenceById(vaccination.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(vaccination.getCompany().id());
        VaccinationJpaEntity saved = jpaRepository.save(
            mapper.toJpa(vaccination, vaccinationType, animal, consultation, company));
        return mapper.toDomain(saved, vaccination.getVaccinationType(),
                                vaccination.getAnimal(), vaccination.getConsultation(),
                                vaccination.getCompany());
    }

    @Override
    public Optional<Vaccination> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Vaccination> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Vaccination> findAllByAnimalId(Long animalId) {
        return jpaRepository.findAllByAnimalId(animalId).stream().map(mapper::toDomain).toList();
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
