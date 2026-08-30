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

    /**
     * La version <strong>exacta</strong> que se le mostro al prospecto, por el par
     * {@code (code, documentVersion)} que devuelve la casilla del front. Resolver
     * "la vigente ahora" en su lugar dejaria la fila aceptada y la fila mostrada
     * separadas en cuanto alguien publique una version entre que se pinta la
     * pantalla y se envia el formulario, y una prueba de cumplimiento que prueba
     * otra cosa es peor que no tenerla.
     */
    Optional<LegalDocumentVersionJpaEntity> findByCodeAndDocumentVersion(String code,
            int documentVersion);

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
