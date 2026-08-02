package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NumberingResolutionJpaRepository
        extends
            JpaRepository<NumberingResolutionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<NumberingResolutionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<NumberingResolutionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    Optional<NumberingResolutionJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = "company")
    List<NumberingResolutionJpaEntity> findAllByCompanyId(Long companyId);

    /**
     * ¿Hay una resolución ACTIVA en ese EXACTO alcance (empresa + sede + tipo)?
     * Base del invariante "una sola resolución activa por (company, sede, tipo)".
     * {@code branchId} null = alcance de EMPRESA (branch_id IS NULL); no null = esa
     * sede concreta. Nativa para el manejo simétrico del null.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM numbering_resolutions WHERE company_id = :companyId "
            + "AND document_type = :documentType AND enabled = true "
            + "AND ((:branchId IS NULL AND branch_id IS NULL) OR branch_id = :branchId)", nativeQuery = true)
    long countActiveByCompanyBranchAndType(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("documentType") String documentType);

    /**
     * Resolución activa a usar para una emisión, BLOQUEADA para actualización (FOR
     * UPDATE): serializa la asignación concurrente del consecutivo. Multi-sucursal
     * (B-6): resuelve la resolución de la SEDE si existe y, si no, la de EMPRESA
     * (branch_id IS NULL) — {@code ORDER BY (branch_id IS
     * NULL)} pone la de sede primero. Nativa para poder usar FOR UPDATE; filtra
     * enabled=true explícito (la @SQLRestriction no aplica a nativas).
     * {@code documentType} es el nombre del enum (columna document_type se persiste
     * como STRING).
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM numbering_resolutions WHERE company_id = :companyId "
            + "AND document_type = :documentType AND enabled = true "
            + "AND (branch_id = :branchId OR branch_id IS NULL) "
            + "ORDER BY (branch_id IS NULL), id LIMIT 1 FOR UPDATE", nativeQuery = true)
    Optional<NumberingResolutionJpaEntity> lockActiveForUpdate(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("documentType") String documentType);

    /**
     * Igual que {@link #lockActiveForUpdate} pero SIN bloqueo (lectura): para los
     * casos que solo necesitan resolución+prefijo y NO consumen consecutivo (POS
     * auto-increment: MATIAS asigna el número). Mismo fallback sede→empresa.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM numbering_resolutions WHERE company_id = :companyId "
            + "AND document_type = :documentType AND enabled = true "
            + "AND (branch_id = :branchId OR branch_id IS NULL) "
            + "ORDER BY (branch_id IS NULL), id LIMIT 1", nativeQuery = true)
    Optional<NumberingResolutionJpaEntity> findActive(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("documentType") String documentType);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE numbering_resolutions SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
