package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDiagnosticImagingTypeRepository implements DiagnosticImagingTypeRepository {
  private final DiagnosticImagingTypeJpaRepository jpaRepository;
  private final DiagnosticImagingTypeJpaMapper mapper;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaDiagnosticImagingTypeRepository(
      DiagnosticImagingTypeJpaRepository jpaRepository,
      DiagnosticImagingTypeJpaMapper mapper,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public DiagnosticImagingType save(DiagnosticImagingType type) {
    CompanyJpaEntity company =
        type.getCompany() == null
            ? null
            : companyJpaRepository.getReferenceById(type.getCompany().id());
    DiagnosticImagingTypeJpaEntity saved = jpaRepository.save(mapper.toJpa(type, company));
    return mapper.toDomain(saved, type.getCompany());
  }

  @Override
  public Optional<DiagnosticImagingType> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<DiagnosticImagingType> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<DiagnosticImagingType> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<DiagnosticImagingType> findAllAvailableForCompany(Long companyId) {
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
