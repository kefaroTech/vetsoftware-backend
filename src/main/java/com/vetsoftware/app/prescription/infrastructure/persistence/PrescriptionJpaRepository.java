package com.vetsoftware.app.prescription.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionJpaRepository extends JpaRepository<PrescriptionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "consultation", "company"})
    List<PrescriptionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "consultation", "company"})
    Optional<PrescriptionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "consultation", "company"})
    Optional<PrescriptionJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE prescriptions
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
