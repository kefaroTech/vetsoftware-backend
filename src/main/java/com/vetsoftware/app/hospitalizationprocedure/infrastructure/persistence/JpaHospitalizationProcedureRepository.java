package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationProcedureRepository implements HospitalizationProcedureRepository {
  private final HospitalizationProcedureJpaRepository jpaRepository;
  private final HospitalizationProcedureJpaMapper mapper;
  private final HospitalizationJpaRepository hospitalizationJpaRepository;
  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaHospitalizationProcedureRepository(
      HospitalizationProcedureJpaRepository jpaRepository,
      HospitalizationProcedureJpaMapper mapper,
      HospitalizationJpaRepository hospitalizationJpaRepository,
      EmployeeJpaRepository employeeJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.hospitalizationJpaRepository = hospitalizationJpaRepository;
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public HospitalizationProcedure save(HospitalizationProcedure procedure) {
    HospitalizationJpaEntity hospitalization =
        hospitalizationJpaRepository.getReferenceById(procedure.getHospitalization().id());
    EmployeeJpaEntity createdBy =
        employeeJpaRepository.getReferenceById(procedure.getCreatedBy().id());
    EmployeeJpaEntity suspensionBy =
        procedure.getSuspensionBy() == null
            ? null
            : employeeJpaRepository.getReferenceById(procedure.getSuspensionBy().id());
    HospitalizationProcedureJpaEntity saved =
        jpaRepository.save(mapper.toJpa(procedure, hospitalization, createdBy, suspensionBy));
    return mapper.toDomain(
        saved,
        procedure.getHospitalization(),
        procedure.getCreatedBy(),
        procedure.getSuspensionBy());
  }

  @Override
  public Optional<HospitalizationProcedure> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<HospitalizationProcedure> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndHospitalization_Company_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<HospitalizationProcedure> findAllByHospitalizationId(Long hospitalizationId) {
    return jpaRepository.findByHospitalizationId(hospitalizationId).stream()
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
