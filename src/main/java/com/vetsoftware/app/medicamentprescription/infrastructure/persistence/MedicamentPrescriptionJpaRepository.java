package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentPrescriptionJpaRepository extends JpaRepository<MedicamentPrescriptionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "prescription")
    List<MedicamentPrescriptionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "prescription")
    Optional<MedicamentPrescriptionJpaEntity> findById(Long id);

    List<MedicamentPrescriptionJpaEntity> findByPrescriptionId(Long prescriptionId);
}
