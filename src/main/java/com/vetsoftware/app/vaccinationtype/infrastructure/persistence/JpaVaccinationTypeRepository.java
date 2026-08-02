package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVaccinationTypeRepository implements VaccinationTypeRepository {
  private final VaccinationTypeJpaRepository jpaRepository;
  private final VaccinationTypeJpaMapper mapper;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaVaccinationTypeRepository(
      VaccinationTypeJpaRepository jpaRepository,
      VaccinationTypeJpaMapper mapper,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public VaccinationType save(VaccinationType vaccinationType) {
    CompanyJpaEntity company =
        vaccinationType.getCompany() == null
            ? null
            : companyJpaRepository.getReferenceById(vaccinationType.getCompany().id());
    VaccinationTypeJpaEntity saved = jpaRepository.save(mapper.toJpa(vaccinationType, company));
    return mapper.toDomain(saved, vaccinationType.getCompany());
  }

  @Override
  public Optional<VaccinationType> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<VaccinationType> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<VaccinationType> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<VaccinationType> findAllAvailableForCompany(Long companyId) {
    return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId).stream()
        .map(mapper::toDomain)
        .toList();
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
