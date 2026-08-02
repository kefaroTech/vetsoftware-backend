package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaEntity;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLaboratoryTestFileRepository implements LaboratoryTestFileRepository {
  private final LaboratoryTestFileJpaRepository jpaRepository;
  private final LaboratoryTestFileJpaMapper mapper;
  private final EmployeeJpaRepository employeeJpaRepository;
  private final LaboratoryTestJpaRepository laboratoryTestJpaRepository;

  public JpaLaboratoryTestFileRepository(
      LaboratoryTestFileJpaRepository jpaRepository,
      LaboratoryTestFileJpaMapper mapper,
      EmployeeJpaRepository employeeJpaRepository,
      LaboratoryTestJpaRepository laboratoryTestJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.employeeJpaRepository = employeeJpaRepository;
    this.laboratoryTestJpaRepository = laboratoryTestJpaRepository;
  }

  @Override
  public LaboratoryTestFile save(LaboratoryTestFile file) {
    EmployeeJpaEntity uploadedBy =
        employeeJpaRepository.getReferenceById(file.getUploadedBy().id());
    LaboratoryTestJpaEntity laboratoryTest =
        laboratoryTestJpaRepository.getReferenceById(file.getLaboratoryTest().id());
    LaboratoryTestFileJpaEntity saved =
        jpaRepository.save(mapper.toJpa(file, uploadedBy, laboratoryTest));
    return mapper.toDomain(saved, file.getUploadedBy(), file.getLaboratoryTest());
  }

  @Override
  public Optional<LaboratoryTestFile> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<LaboratoryTestFile> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndLaboratoryTest_Company_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<LaboratoryTestFile> findAllByLaboratoryTestId(Long laboratoryTestId) {
    return jpaRepository.findAllByLaboratoryTest_Id(laboratoryTestId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public void delete(Long id) {
    jpaRepository.deleteById(id);
  }
}
