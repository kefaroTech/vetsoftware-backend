package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentPrescriptionJpaRepository
    extends JpaRepository<MedicamentPrescriptionJpaEntity, Long> {

  @Override
  @EntityGraph(attributePaths = {"prescription", "medicament"})
  List<MedicamentPrescriptionJpaEntity> findAll();

  @Override
  @EntityGraph(attributePaths = {"prescription", "medicament"})
  Optional<MedicamentPrescriptionJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = {"prescription", "medicament"})
  Optional<MedicamentPrescriptionJpaEntity> findByIdAndPrescription_Company_Id(
      Long id, Long companyId);

  List<MedicamentPrescriptionJpaEntity> findByPrescriptionId(Long prescriptionId);

  boolean existsByMedicament_Id(Long medicamentId);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE medicament_prescriptions SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

  boolean existsByPrescription_Id(Long prescriptionId);
}
