package com.vetsoftware.app.vaccination.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVaccinationRepository implements VaccinationRepository {

    private static final int BY_ANIMAL_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_ANIMAL_MAX_PAGE_SIZE = 200;
    private final VaccinationJpaRepository jpaRepository;
    private final VaccinationJpaMapper mapper;
    private final VaccinationTypeJpaRepository vaccinationTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaVaccinationRepository(VaccinationJpaRepository jpaRepository,
            VaccinationJpaMapper mapper, VaccinationTypeJpaRepository vaccinationTypeJpaRepository,
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
        VaccinationTypeJpaEntity vaccinationType = vaccinationTypeJpaRepository
                .getReferenceById(vaccination.getVaccinationType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(vaccination.getAnimal().id());
        ConsultationJpaEntity consultation = vaccination.getConsultation() == null
                ? null
                : consultationJpaRepository.getReferenceById(vaccination.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(vaccination.getCompany().id());
        VaccinationJpaEntity saved = jpaRepository
                .save(mapper.toJpa(vaccination, vaccinationType, animal, consultation, company));
        return mapper.toDomain(saved, vaccination.getVaccinationType(), vaccination.getAnimal(),
                vaccination.getConsultation(), vaccination.getCompany());
    }

    @Override
    public Optional<Vaccination> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Vaccination> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Vaccination> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Vaccination> findAllByAnimalId(Long animalId, int page, int pageSize) {
        Page<VaccinationJpaEntity> result = jpaRepository.findAllByAnimalId(animalId,
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
