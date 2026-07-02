package com.vetsoftware.app.animal.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeightRecordJpaRepository extends JpaRepository<WeightRecordJpaEntity, Long> {

    @EntityGraph(attributePaths = {"animal", "company"})
    List<WeightRecordJpaEntity> findByAnimal_IdAndCompany_IdOrderByMeasuredAtDescIdDesc(Long animalId, Long companyId);

    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<WeightRecordJpaEntity> findTopByAnimal_IdAndCompany_IdOrderByMeasuredAtDescIdDesc(Long animalId, Long companyId);

    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<WeightRecordJpaEntity> findByIdAndAnimal_IdAndCompany_Id(Long id, Long animalId, Long companyId);

    Optional<WeightRecordJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // --- Enriquecimiento del peso actual del animal (proyección ligera, sin cargar asociaciones) ---
    // Las queries nativas NO respetan @SQLRestriction, por eso el `enabled = true` es explícito.

    @Query(value = "SELECT w.animal_id AS animalId, w.weight_value AS value, w.weight_unit AS unit, "
        + "w.measured_at AS measuredAt FROM weight_records w "
        + "WHERE w.animal_id = :animalId AND w.company_id = :companyId AND w.enabled = true "
        + "ORDER BY w.measured_at DESC, w.id DESC LIMIT 1", nativeQuery = true)
    Optional<LatestWeightProjection> findLatestByAnimalId(@Param("animalId") Long animalId,
                                                          @Param("companyId") Long companyId);

    /** Último registro por animal para un conjunto de ids, en una sola query (evita N+1 en listados). */
    @Query(value = "SELECT w.animal_id AS animalId, w.weight_value AS value, w.weight_unit AS unit, "
        + "w.measured_at AS measuredAt FROM weight_records w "
        + "WHERE w.company_id = :companyId AND w.enabled = true AND w.animal_id IN (:animalIds) "
        + "AND w.id = (SELECT w2.id FROM weight_records w2 "
        + "WHERE w2.animal_id = w.animal_id AND w2.enabled = true "
        + "ORDER BY w2.measured_at DESC, w2.id DESC LIMIT 1)", nativeQuery = true)
    List<LatestWeightProjection> findLatestByAnimalIds(@Param("companyId") Long companyId,
                                                       @Param("animalIds") Collection<Long> animalIds);

    interface LatestWeightProjection {
        Long getAnimalId();
        BigDecimal getValue();
        String getUnit();
        LocalDate getMeasuredAt();
    }
}
