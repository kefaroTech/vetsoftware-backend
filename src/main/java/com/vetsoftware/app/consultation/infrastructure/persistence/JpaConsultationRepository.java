package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConsultationRepository implements ConsultationRepository {
  private final ConsultationJpaRepository jpaRepository;
  private final ConsultationJpaMapper mapper;
  private final ConsultationTypeJpaRepository consultationTypeJpaRepository;
  private final AnimalJpaRepository animalJpaRepository;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaConsultationRepository(
      ConsultationJpaRepository jpaRepository,
      ConsultationJpaMapper mapper,
      ConsultationTypeJpaRepository consultationTypeJpaRepository,
      AnimalJpaRepository animalJpaRepository,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.consultationTypeJpaRepository = consultationTypeJpaRepository;
    this.animalJpaRepository = animalJpaRepository;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public Consultation save(Consultation consultation) {
    ConsultationTypeJpaEntity consultationType =
        consultationTypeJpaRepository.getReferenceById(consultation.getConsultationType().id());
    AnimalJpaEntity animal = animalJpaRepository.getReferenceById(consultation.getAnimal().id());
    CompanyJpaEntity company =
        companyJpaRepository.getReferenceById(consultation.getCompany().id());
    ConsultationJpaEntity saved =
        jpaRepository.save(mapper.toJpa(consultation, consultationType, animal, company));
    return mapper.toDomain(
        saved,
        consultation.getConsultationType(),
        consultation.getAnimal(),
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
  public List<Consultation> findAllByCompanyId(Long companyId) {
    return jpaRepository.findAllByCompany_Id(companyId).stream().map(mapper::toDomain).toList();
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
