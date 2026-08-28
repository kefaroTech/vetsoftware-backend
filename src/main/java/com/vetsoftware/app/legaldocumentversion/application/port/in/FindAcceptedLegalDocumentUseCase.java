package com.vetsoftware.app.legaldocumentversion.application.port.in;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>Relee por huella el texto que se acepto.</strong>
 *
 * <p>
 * Este es el puerto que convierte la columna {@code content_hash} en una prueba
 * util. Una aceptacion guarda el hash del texto que el cliente leyo; sin una
 * operacion que devuelva <em>ese</em> texto, el cliente tendria una huella y
 * ninguna forma de exhibir lo que acepto —y una prueba que no se puede exhibir
 * no le sirve—. Busca por {@code (code, contentHash)}, que es exactamente el
 * indice {@code uq_ldv_content}, asi que da la version historica aunque ya haya
 * sido sucedida.
 */
public interface FindAcceptedLegalDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('legaldocument.read') and @authz.isMyCompany(#companyId))")
    LegalDocumentVersionDto findByCodeAndHash(String code, String contentHash, Long companyId);
}
