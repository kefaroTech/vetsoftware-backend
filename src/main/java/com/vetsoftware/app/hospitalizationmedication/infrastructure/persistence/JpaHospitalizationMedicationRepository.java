package com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationMedicationRepository implements HospitalizationMedicationRepository {
  private final HospitalizationMedicationJpaRepository jpaRepository;
  private final HospitalizationMedicationJpaMapper mapper;
  private final HospitalizationJpaRepository hospitalizationJpaRepository;
  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaHospitalizationMedicationRepository(
      HospitalizationMedicationJpaRepository jpaRepository,
      HospitalizationMedicationJpaMapper mapper,
      HospitalizationJpaRepository hospitalizationJpaRepository,
      EmployeeJpaRepository employeeJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.hospitalizationJpaRepository = hospitalizationJpaRepository;
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public HospitalizationMedication save(HospitalizationMedication medication) {
    HospitalizationJpaEntity hospitalization =
        hospitalizationJpaRepository.getReferenceById(medication.getHospitalization().id());
    EmployeeJpaEntity createdBy =
        employeeJpaRepository.getReferenceById(medication.getCreatedBy().id());
    EmployeeJpaEntity suspensionBy =
        medication.getSuspensionBy() == null
            ? null
            : employeeJpaRepository.getReferenceById(medication.getSuspensionBy().id());
    HospitalizationMedicationJpaEntity saved =
        jpaRepository.save(mapper.toJpa(medication, hospitalization, createdBy, suspensionBy));
    return mapper.toDomain(
        saved,
        medication.getHospitalization(),
        medication.getCreatedBy(),
        medication.getSuspensionBy());
  }

  @Override
  public Optional<HospitalizationMedication> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<HospitalizationMedication> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndHospitalization_Company_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<HospitalizationMedication> findAllByHospitalizationId(Long hospitalizationId) {
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
