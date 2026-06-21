package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NumberingResolutionJpaRepository extends JpaRepository<NumberingResolutionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<NumberingResolutionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<NumberingResolutionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<NumberingResolutionJpaEntity> findAllByCompanyId(Long companyId);

    /**
     * ¿La empresa ya tiene una resolución ACTIVA para ese tipo de documento? Base del invariante "una sola
     * resolución activa por (company, tipo)". `enabled = true` explícito además del {@code @SQLRestriction}.
     */
    boolean existsByCompany_IdAndDocumentTypeAndEnabledTrue(
        Long companyId, com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType documentType);

    /**
     * Resolución activa de la empresa para un tipo de documento, BLOQUEADA para actualización (FOR UPDATE):
     * serializa la asignación concurrente del consecutivo entre emisiones de la misma empresa+tipo. Nativa
     * para poder usar FOR UPDATE; filtra enabled=true explícitamente (la @SQLRestriction no aplica a nativas).
     * {@code documentType} es el nombre del enum (columna document_type se persiste como STRING).
     */
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM numbering_resolutions WHERE company_id = :companyId "
              + "AND document_type = :documentType AND enabled = true ORDER BY id LIMIT 1 FOR UPDATE",
        nativeQuery = true)
    Optional<NumberingResolutionJpaEntity> lockActiveForUpdate(
        @org.springframework.data.repository.query.Param("companyId") Long companyId,
        @org.springframework.data.repository.query.Param("documentType") String documentType);

    /**
     * Resolución activa de la empresa para un tipo de documento, SIN bloqueo (lectura). Para los casos que
     * solo necesitan resolución+prefijo y NO consumen consecutivo (POS auto-increment: MATIAS asigna el
     * número). Nativa por consistencia con {@link #lockActiveForUpdate} y para filtrar enabled=true explícito.
     */
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM numbering_resolutions WHERE company_id = :companyId "
              + "AND document_type = :documentType AND enabled = true ORDER BY id LIMIT 1",
        nativeQuery = true)
    Optional<NumberingResolutionJpaEntity> findActive(
        @org.springframework.data.repository.query.Param("companyId") Long companyId,
        @org.springframework.data.repository.query.Param("documentType") String documentType);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE numbering_resolutions SET enabled = true WHERE id = :id",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
