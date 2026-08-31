package com.vetsoftware.app.catalogitemaihint.application.port.in;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La pista que rige hoy para un articulo.
 *
 * <p>
 * Recibe un id que el cliente escribe en la URL, asi que la mira
 * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}: la salida correcta ahi es
 * {@code hasRole('SYSTEM')}, porque la fila no pertenece a ninguna empresa y no
 * hay {@code companyId} que revalidar.
 */
public interface FindCurrentCatalogItemAiHintUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemAiHintDto findCurrentByCatalogItemId(Long catalogItemId);
}
