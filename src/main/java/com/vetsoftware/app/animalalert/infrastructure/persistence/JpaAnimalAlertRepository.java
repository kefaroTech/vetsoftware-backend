package com.vetsoftware.app.animalalert.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAnimalAlertRepository implements AnimalAlertRepository {
  private final AnimalAlertJpaRepository jpaRepository;
  private final AnimalAlertJpaMapper mapper;
  private final AnimalJpaRepository animalJpaRepository;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaAnimalAlertRepository(
      AnimalAlertJpaRepository jpaRepository,
      AnimalAlertJpaMapper mapper,
      AnimalJpaRepository animalJpaRepository,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.animalJpaRepository = animalJpaRepository;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public AnimalAlert save(AnimalAlert alert) {
    AnimalJpaEntity animal = animalJpaRepository.getReferenceById(alert.getAnimal().id());
    CompanyJpaEntity company = companyJpaRepository.getReferenceById(alert.getCompany().id());
    AnimalAlertJpaEntity saved = jpaRepository.save(mapper.toJpa(alert, animal, company));
    return mapper.toDomain(saved, alert.getAnimal(), alert.getCompany());
  }

  @Override
  public Optional<AnimalAlert> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<AnimalAlert> findByAnimalIdAndCompanyId(Long animalId, Long companyId) {
    return jpaRepository
        .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(animalId, companyId)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public void delete(Long id, Long companyId) {
    jpaRepository.findByIdAndCompany_Id(id, companyId).ifPresent(jpaRepository::delete);
  }
}
