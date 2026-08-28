package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.ListTaxReturnsUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Los tres listados de declaraciones.
 *
 * <p>
 * Ninguno filtra por empresa porque {@code tax_returns} no tiene esa columna, y
 * por eso los tres puertos van cerrados a {@code hasRole('SYSTEM')} a secas —
 * la unica salida que admite {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 *
 * <p>
 * {@link #listBecomingFinalBefore(LocalDate, int, int)} es el que sostiene la
 * politica de conservacion: sirve a {@code ix_tax_returns_firmeza} y de el sale
 * hasta cuando no se puede purgar el detalle de uso.
 */
@Observed(name = "tax.return.list")
@Service
public class ListTaxReturnsService implements ListTaxReturnsUseCase {

    private final TaxReturnRepository repository;

    public ListTaxReturnsService(TaxReturnRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<TaxReturnDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(TaxReturnDto::from);
    }

    @Override
    public PageResult<TaxReturnDto> listByFiscalPeriod(String fiscalPeriodKey, int page,
            int pageSize) {
        return repository.findAllByFiscalPeriodKey(fiscalPeriodKey, page, pageSize)
                .map(TaxReturnDto::from);
    }

    @Override
    public PageResult<TaxReturnDto> listBecomingFinalBefore(LocalDate limit, int page,
            int pageSize) {
        return repository.findAllByFirmezaUntilBefore(limit, page, pageSize)
                .map(TaxReturnDto::from);
    }
}
