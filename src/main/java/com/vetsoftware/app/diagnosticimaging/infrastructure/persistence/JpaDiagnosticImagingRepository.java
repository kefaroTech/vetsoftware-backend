package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDiagnosticImagingRepository implements DiagnosticImagingRepository {

    private static final int BY_ANIMAL_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_ANIMAL_MAX_PAGE_SIZE = 200;
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
        DiagnosticImagingTypeJpaEntity type = diagnosticImagingTypeJpaRepository
                .getReferenceById(imaging.getDiagnosticImagingType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(imaging.getAnimal().id());
        ConsultationJpaEntity consultation = imaging.getConsultation() == null
                ? null
                : consultationJpaRepository.getReferenceById(imaging.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(imaging.getCompany().id());
        DiagnosticImagingJpaEntity saved = jpaRepository
                .save(mapper.toJpa(imaging, type, animal, consultation, company));
        return mapper.toDomain(saved, imaging.getDiagnosticImagingType(), imaging.getAnimal(),
                imaging.getConsultation(), imaging.getCompany());
    }

    @Override
    public Optional<DiagnosticImaging> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DiagnosticImaging> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<DiagnosticImaging> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<DiagnosticImaging> findAllByAnimalIdAndCompanyId(Long animalId,
            Long companyId, String query, int page, int pageSize) {
        Page<DiagnosticImagingJpaEntity> result = jpaRepository.findAllByAnimalIdAndCompanyId(
                animalId, companyId, query, byAnimalPageRequest(page, pageSize));
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
