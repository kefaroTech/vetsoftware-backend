package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** El calendario contable completo, del mes mas reciente hacia atras. */
public interface ListAccountingPeriodsUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas y sin filtro de empresa que
     * ofrecer.</strong> No es que este listado se olvidara del {@code companyId}:
     * es que la tabla no tiene ninguno, y tampoco existe un caso de uso hermano
     * acotado por empresa que pudiera servir lo mismo a un tenant. Cualquier otra
     * expresion aqui —incluida una acotada por una clave foranea ajena— caeria en
     * lo que persigue {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<AccountingPeriodDto> listAll(int page, int pageSize);
}
