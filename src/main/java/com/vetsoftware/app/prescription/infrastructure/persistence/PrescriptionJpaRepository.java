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

    /** Camino SYSTEM: sin empresa que acotar. Nunca desde un empleado. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE prescriptions
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * En la reactivacion no hay lectura previa que valide la propiedad —el servicio
     * decide si existe mirando las filas afectadas—, asi que un UPDATE por id a
     * secas resucitaba la receta anulada de cualquier tenant para quien conociera
     * el id. El resto de la feature ya pasaba la empresa; solo esto quedo fuera.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE prescriptions
            SET enabled = true
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
