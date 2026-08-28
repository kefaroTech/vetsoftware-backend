package com.vetsoftware.app.legaldocumentversion.application.port.out;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * No declara ningun {@code update}: lo unico que este puerto sabe hacer sobre
 * una fila viva es {@code save} de una version que acaba de ser sucedida, que
 * mueve {@code superseded_at} y nada mas.
 */
public interface LegalDocumentVersionRepository {

    LegalDocumentVersion save(LegalDocumentVersion version);

    /**
     * Cierra la vigencia de una version <strong>y la escribe en la base antes de
     * devolver</strong>.
     *
     * <p>
     * Existe aparte de {@link #save} por una razon medida, no por estilo. El indice
     * {@code uq_ldv_current} solo admite una version vigente por documento, y al
     * publicar hay un instante con dos: la que se sucede y la que entra. Hibernate
     * ordena el vaciado con <b>los INSERT antes que los UPDATE</b>, asi que dejar
     * las dos escrituras al flush de la transaccion emite primero el INSERT de la
     * nueva —cuando la vieja todavia tiene {@code superseded_at} nulo— y MySQL
     * rechaza la publicacion entera con un «Duplicate entry … for key
     * uq_ldv_current». Este metodo garantiza que el cierre llega antes.
     */
    LegalDocumentVersion supersede(LegalDocumentVersion version);

    Optional<LegalDocumentVersion> findById(Long id);

    Optional<LegalDocumentVersion> findCurrentByCode(String code);

    /** La lectura por huella: da la version historica aunque ya este sucedida. */
    Optional<LegalDocumentVersion> findByCodeAndContentHash(String code, String contentHash);

    boolean existsByCodeAndContentHash(String code, String contentHash);

    Optional<Integer> findLastDocumentVersion(String code);

    PageResult<LegalDocumentVersion> findAllByCode(String code, int page, int pageSize);
}
