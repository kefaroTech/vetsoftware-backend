package com.vetsoftware.app.catalogitemaihint.application.port.in;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Las pistas vigentes, una por articulo que tenga.
 *
 * <p>
 * <strong>{@code hasRole('SYSTEM')} a secas, y es la unica forma
 * correcta.</strong> El listado no filtra por empresa porque no hay ninguna que
 * filtrar: {@code catalog_item_ai_hints} no tiene {@code company_id} ni alcanza
 * {@code companies}. Eso es exactamente el caso de
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} —regla dura—, que exige el rol
 * pelado y no admite una disyuncion con {@code hasAuthority}: bastaria sembrar
 * esa authority en un rol de empresa para que un empleado leyera —y por el
 * mismo camino editara— las instrucciones del modelo comercial de la
 * plataforma.
 */
public interface ListCurrentCatalogItemAiHintsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CatalogItemAiHintDto> listCurrent(int page, int pageSize);
}
