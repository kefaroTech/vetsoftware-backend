package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.dto.PageResult;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConsultationRepository implements ConsultationRepository {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private final ConsultationJpaRepository jpaRepository;
    private final ConsultationJpaMapper mapper;
    private final ConsultationTypeJpaRepository consultationTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaConsultationRepository(ConsultationJpaRepository jpaRepository,
            ConsultationJpaMapper mapper,
            ConsultationTypeJpaRepository consultationTypeJpaRepository,
            AnimalJpaRepository animalJpaRepository, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.consultationTypeJpaRepository = consultationTypeJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Consultation save(Consultation consultation) {
        ConsultationTypeJpaEntity consultationType = consultationTypeJpaRepository
                .getReferenceById(consultation.getConsultationType().id());
        AnimalJpaEntity animal = animalJpaRepository
                .getReferenceById(consultation.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(consultation.getCompany().id());
        ConsultationJpaEntity saved = jpaRepository
                .save(mapper.toJpa(consultation, consultationType, animal, company));
        return mapper.toDomain(saved, consultation.getConsultationType(), consultation.getAnimal(),
                consultation.getCompany());
    }

    @Override
    public Optional<Consultation> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Consultation> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Consultation> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Consultation> findAllByCompanyId(Long companyId, int page, int pageSize) {
        Page<ConsultationJpaEntity> result = jpaRepository.findAllByCompany_Id(companyId,
                pageRequest(page, pageSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente: una pagina negativa o un tamano desmedido
     * no deben poder volver a pedir la tabla entera, que es el fallo que se esta
     * corrigiendo. El orden por id descendente es estable y devuelve primero lo mas
     * reciente; sin orden explicito la paginacion no es determinista y una misma
     * fila puede salir en dos paginas.
     */
    private static PageRequest pageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndCompany_Id(id, companyId).ifPresent(jpaRepository::delete);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
