package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ElectronicDocumentJpaRepository
        extends
            JpaRepository<ElectronicDocumentJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    // Multi-sucursal (Fase C): lista de la empresa con filtro OPCIONAL por sede.
    // branchId es una
    // columna Long
    // pelada (no @ManyToOne), por eso el guard es sobre e.branchId. null = todas
    // las sedes.
    /**
     * Paso 1 de la paginación: solo los ids, sin traer colecciones.
     *
     * <p>
     * No se pagina directamente la consulta con el grafo porque {@code lines} y
     * {@code payments} son colecciones: un JOIN FETCH to-many multiplica las filas
     * y Hibernate, al no poder recortar en SQL, se trae TODO y pagina en memoria
     * (HHH000104). Es decir, parecería que funciona sin arreglar nada. Contando ids
     * no hay producto cartesiano y el LIMIT es real.
     */
    @Query("""
            SELECT e.id
            FROM ElectronicDocumentJpaEntity e
            WHERE e.company.id = :companyId
              AND (:branchId IS NULL OR e.branchId = :branchId)
              AND (:documentType IS NULL OR e.documentType = :documentType)
              AND (:dianStatus IS NULL OR e.dianStatus = :dianStatus)
            """)
    Page<Long> findIdsByCompanyIdAndOptionalBranch(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            // BE-06: la pantalla filtraba por estos dos EN CLIENTE sobre la lista
            // completa. Sin ellos aqui no se puede paginar sin ocultar documentos.
            @Param("documentType") ElectronicDocumentType documentType,
            @Param("dianStatus") DianStatus dianStatus, Pageable pageable);

    /**
     * Paso 2: hidrata con sus colecciones solo los documentos de la página ya
     * elegida.
     */
    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    @Query("""
            SELECT DISTINCT e
            FROM ElectronicDocumentJpaEntity e
            WHERE e.id IN :ids
            """)
    List<ElectronicDocumentJpaEntity> findAllByIdsWithDetails(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    @Query("""
            SELECT e
            FROM ElectronicDocumentJpaEntity e
            WHERE e.company.id = :companyId
              AND (:branchId IS NULL OR e.branchId = :branchId)
            """)
    List<ElectronicDocumentJpaEntity> findByCompanyIdAndOptionalBranch(
            @Param("companyId") Long companyId, @Param("branchId") Long branchId);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findByCufeAndCompany_Id(String cufe, Long companyId);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findByOpenAccountIdAndCompany_Id(Long openAccountId,
            Long companyId);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    List<ElectronicDocumentJpaEntity> findByDianStatus(DianStatus dianStatus);

    @Query("""
            SELECT COUNT(e)
            FROM ElectronicDocumentJpaEntity e
            WHERE e.dianStatus = :status
              AND e.createdDate >= :from
            """)
    long countBacklogSince(@Param("status") DianStatus status, @Param("from") LocalDateTime from);

    @Query("""
            SELECT COUNT(e)
            FROM ElectronicDocumentJpaEntity e
            WHERE e.dianStatus = :status
              AND e.createdDate >= :from
              AND e.createdDate < :to
            """)
    long countBacklogBetween(@Param("status") DianStatus status, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(e)
            FROM ElectronicDocumentJpaEntity e
            WHERE e.dianStatus = :status
              AND e.createdDate < :before
            """)
    long countBacklogBefore(@Param("status") DianStatus status,
            @Param("before") LocalDateTime before);

    boolean existsByOpenAccountId(Long openAccountId);

    boolean existsByOpenAccountIdAndDocumentType(Long openAccountId,
            com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType documentType);

    @EntityGraph(attributePaths = {"company", "lines", "payments"})
    Optional<ElectronicDocumentJpaEntity> findByCompany_IdAndClientRequestId(Long companyId,
            String clientRequestId);
}
