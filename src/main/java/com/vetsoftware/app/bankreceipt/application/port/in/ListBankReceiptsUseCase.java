package com.vetsoftware.app.bankreceipt.application.port.in;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** El extracto completo, en cualquier estado. Barrido de plataforma. */
public interface ListBankReceiptsUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas y sin filtro de empresa que
     * ofrecer.</strong> No es que este listado se olvidara del {@code companyId}:
     * es que la tabla no tiene ninguno. Cualquier otra expresion aqui —incluida una
     * acotada por una clave foranea ajena— caeria en lo que persigue
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<BankReceiptDto> listAll(int page, int pageSize);
}
