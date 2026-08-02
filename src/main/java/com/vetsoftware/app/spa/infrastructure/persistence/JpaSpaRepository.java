package com.vetsoftware.app.spa.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaEntity;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSpaRepository implements SpaRepository {
  private final SpaJpaRepository jpaRepository;
  private final SpaJpaMapper mapper;
  private final SpaTypeJpaRepository spaTypeJpaRepository;
  private final AnimalJpaRepository animalJpaRepository;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaSpaRepository(
      SpaJpaRepository jpaRepository,
      SpaJpaMapper mapper,
      SpaTypeJpaRepository spaTypeJpaRepository,
      AnimalJpaRepository animalJpaRepository,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.spaTypeJpaRepository = spaTypeJpaRepository;
    this.animalJpaRepository = animalJpaRepository;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public Spa save(Spa spa) {
    SpaTypeJpaEntity spaType = spaTypeJpaRepository.getReferenceById(spa.getSpaType().id());
    AnimalJpaEntity animal = animalJpaRepository.getReferenceById(spa.getAnimal().id());
    CompanyJpaEntity company = companyJpaRepository.getReferenceById(spa.getCompany().id());
    SpaJpaEntity saved = jpaRepository.save(mapper.toJpa(spa, spaType, animal, company));
    return mapper.toDomain(saved, spa.getSpaType(), spa.getAnimal(), spa.getCompany());
  }

  @Override
  public Optional<Spa> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Spa> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<Spa> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Spa> findAllByAnimalId(Long animalId) {
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
