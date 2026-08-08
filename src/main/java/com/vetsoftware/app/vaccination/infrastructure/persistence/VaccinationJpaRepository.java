package com.vetsoftware.app.vaccination.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VaccinationJpaRepository extends JpaRepository<VaccinationJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"vaccinationType", "animal", "consultation", "company"})
    List<VaccinationJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"vaccinationType", "animal", "consultation", "company"})
    Optional<VaccinationJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"vaccinationType", "animal", "consultation", "company"})
    Optional<VaccinationJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"vaccinationType", "animal", "consultation", "company"})
    @Query("SELECT x FROM VaccinationJpaEntity x WHERE x.animal.id = :animalId "
            + "AND (:q IS NULL OR :q = '' OR LOWER(x.lot) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.notes) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.applicationSite) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<VaccinationJpaEntity> findAllByAnimalId(@Param("animalId") Long animalId,
            @Param("q") String q, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE vaccinations SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByVaccinationType_Id(Long vaccinationTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
