package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDiagnosticImagingRepository implements DiagnosticImagingRepository {
    private final DiagnosticImagingJpaRepository jpaRepository;
    private final DiagnosticImagingJpaMapper mapper;
    private final DiagnosticImagingTypeJpaRepository diagnosticImagingTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaDiagnosticImagingRepository(DiagnosticImagingJpaRepository jpaRepository,
                                          DiagnosticImagingJpaMapper mapper,
                                          DiagnosticImagingTypeJpaRepository diagnosticImagingTypeJpaRepository,
                                          AnimalJpaRepository animalJpaRepository,
                                          ConsultationJpaRepository consultationJpaRepository,
                                          CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.diagnosticImagingTypeJpaRepository = diagnosticImagingTypeJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public DiagnosticImaging save(DiagnosticImaging imaging) {
        DiagnosticImagingTypeJpaEntity type =
            diagnosticImagingTypeJpaRepository.getReferenceById(imaging.getDiagnosticImagingType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(imaging.getAnimal().id());
        ConsultationJpaEntity consultation = imaging.getConsultation() == null ? null
            : consultationJpaRepository.getReferenceById(imaging.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(imaging.getCompany().id());
        DiagnosticImagingJpaEntity saved = jpaRepository.save(
            mapper.toJpa(imaging, type, animal, consultation, company));
        return mapper.toDomain(saved, imaging.getDiagnosticImagingType(),
            imaging.getAnimal(), imaging.getConsultation(), imaging.getCompany());
    }

    @Override
    public Optional<DiagnosticImaging> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DiagnosticImaging> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DiagnosticImaging> findAllByAnimalId(Long animalId) {
        return jpaRepository.findAllByAnimalId(animalId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
