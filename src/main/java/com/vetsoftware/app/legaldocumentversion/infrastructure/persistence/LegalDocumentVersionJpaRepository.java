package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LegalDocumentVersionJpaRepository
        extends
            JpaRepository<LegalDocumentVersionJpaEntity, Long> {

    /**
     * La vigente. {@code superseded_at IS NULL} es la misma condicion que sostiene
     * {@code uq_ldv_current} a traves de la columna generada, asi que el indice
     * garantiza que hay como mucho una.
     */
    Optional<LegalDocumentVersionJpaEntity> findByCodeAndSupersededAtIsNull(String code);

    Optional<LegalDocumentVersionJpaEntity> findByCodeAndContentHash(String code,
            String contentHash);

    boolean existsByCodeAndContentHash(String code, String contentHash);

    Page<LegalDocumentVersionJpaEntity> findByCode(String code, Pageable pageable);

    /**
     * El ultimo numero de version de negocio publicado. Es un {@code SELECT}
     * agregado, no una mutacion.
     */
    @Query("select max(v.documentVersion) from LegalDocumentVersionJpaEntity v "
            + "where v.code = :code")
    Optional<Integer> findLastDocumentVersion(String code);
}
