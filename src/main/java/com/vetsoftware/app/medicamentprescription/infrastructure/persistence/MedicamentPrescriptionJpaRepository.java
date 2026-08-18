package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentPrescriptionJpaRepository
        extends
            JpaRepository<MedicamentPrescriptionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"prescription", "medicament"})
    List<MedicamentPrescriptionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"prescription", "medicament"})
    Optional<MedicamentPrescriptionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"prescription", "medicament"})
    Optional<MedicamentPrescriptionJpaEntity> findByIdAndPrescription_Company_Id(Long id,
            Long companyId);

    List<MedicamentPrescriptionJpaEntity> findByPrescriptionId(Long prescriptionId);

    boolean existsByMedicament_Id(Long medicamentId);

    /** Camino SYSTEM: sin empresa que acotar. Nunca desde un empleado. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE medicament_prescriptions
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * La linea de receta no tiene {@code company_id} propio: su empresa es la de la
     * receta, y por eso el filtro es un EXISTS que sube hasta ella. No es defensa
     * en profundidad: en la reactivacion no hay lectura previa que valide la
     * propiedad —el servicio decide si existe mirando las filas afectadas—, asi que
     * un UPDATE por id a secas resucitaba la linea anulada de la receta de
     * cualquier tenant para quien conociera el id.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE medicament_prescriptions
            SET enabled = true
            WHERE id = :id
              AND EXISTS (SELECT 1 FROM prescriptions p
                          WHERE p.id = medicament_prescriptions.prescription_id
                            AND p.company_id = :companyId)
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByPrescription_Id(Long prescriptionId);
}
