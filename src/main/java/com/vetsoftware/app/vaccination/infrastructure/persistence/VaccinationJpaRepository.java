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
    @Query("""
            SELECT x
            FROM VaccinationJpaEntity x
            WHERE x.animal.id = :animalId
              AND x.company.id = :companyId
              AND (:q IS NULL OR :q = '' OR LOWER(x.lot) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.notes) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(x.applicationSite) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<VaccinationJpaEntity> findAllByAnimalIdAndCompanyId(@Param("animalId") Long animalId,
            @Param("companyId") Long companyId, @Param("q") String q, Pageable pageable);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * Un UPDATE por id a secas reactivaba el registro borrado de cualquier tenant
     * para quien conociera el id, porque no hay ninguna lectura previa que valide
     * la propiedad — el servicio decide si existe mirando las filas afectadas.
     * <p>
     * Y sube {@code version} porque este UPDATE nativo va directo a la base: no
     * comprueba ni incrementa la version, que {@code @Version} solo protege en el
     * ciclo leer-modificar-guardar. Un {@code save} concurrente cargado antes
     * reescribe la fila entera desde el dominio, con su {@code enabled = false}, y
     * su {@code WHERE version = ?} casa igual, deshaciendo la reactivacion en
     * silencio. Movida la version, ese save ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}. La {@code version} NO va en el
     * {@code WHERE}: reactivar es deliberado y debe ejecutarse siempre, no competir
     * con una edicion.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE vaccinations
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByVaccinationType_Id(Long vaccinationTypeId);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByConsultation_Id(Long consultationId);

    boolean existsByCompany_Id(Long companyId);
}
