package com.vetsoftware.app.surgery.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaEntity;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSurgeryRepository implements SurgeryRepository {
    private final SurgeryJpaRepository jpaRepository;
    private final SurgeryJpaMapper mapper;
    private final SurgeryTypeJpaRepository surgeryTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSurgeryRepository(SurgeryJpaRepository jpaRepository, SurgeryJpaMapper mapper,
            SurgeryTypeJpaRepository surgeryTypeJpaRepository,
            AnimalJpaRepository animalJpaRepository,
            ConsultationJpaRepository consultationJpaRepository,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.surgeryTypeJpaRepository = surgeryTypeJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Surgery save(Surgery surgery) {
        SurgeryTypeJpaEntity surgeryType = surgeryTypeJpaRepository
                .getReferenceById(surgery.getSurgeryType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(surgery.getAnimal().id());
        ConsultationJpaEntity consultation = surgery.getConsultation() == null
                ? null
                : consultationJpaRepository.getReferenceById(surgery.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(surgery.getCompany().id());
        SurgeryJpaEntity saved = jpaRepository
                .save(mapper.toJpa(surgery, surgeryType, animal, consultation, company));
        return mapper.toDomain(saved, surgery.getSurgeryType(), surgery.getAnimal(),
                surgery.getConsultation(), surgery.getCompany());
    }

    @Override
    public Optional<Surgery> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Surgery> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Surgery> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Surgery> findAllByAnimalId(Long animalId) {
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
