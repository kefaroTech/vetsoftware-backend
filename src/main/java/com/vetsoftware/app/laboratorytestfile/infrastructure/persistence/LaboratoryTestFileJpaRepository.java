package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryTestFileJpaRepository
    extends JpaRepository<LaboratoryTestFileJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = {"uploadedBy", "laboratoryTest"})
  Optional<LaboratoryTestFileJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = {"uploadedBy", "laboratoryTest"})
  Optional<LaboratoryTestFileJpaEntity> findByIdAndLaboratoryTest_Company_Id(
      Long id, Long companyId);

  @EntityGraph(attributePaths = {"uploadedBy", "laboratoryTest"})
  List<LaboratoryTestFileJpaEntity> findAllByLaboratoryTest_Id(Long laboratoryTestId);
}
