package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El embudo comercial completo: todas las cotizaciones, de todas las empresas y
 * de todos los prospectos.
 *
 * <p>
 * NO filtra por empresa y por eso va cerrado a hasRole('SYSTEM') A SECAS, sin
 * ninguna otra alternativa en el OR (LISTADOS_SIN_EMPRESA_SOLO_SYSTEM, BE-29).
 * Lo que necesita el tenant vive en {@link ListQuotesByCompanyUseCase}, que si
 * recibe companyId.
 */
public interface ListQuotesUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<QuoteSummaryDto> listAll(int page, int pageSize);
}
