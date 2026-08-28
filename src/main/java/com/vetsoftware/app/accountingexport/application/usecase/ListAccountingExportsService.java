package com.vetsoftware.app.accountingexport.application.usecase;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.in.ListAccountingExportsUseCase;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Las dos bandejas: la del mes y la completa.
 *
 * <p>
 * Ninguna filtra por empresa porque {@code accounting_exports} no tiene esa
 * columna, y por eso los dos puertos van cerrados a {@code hasRole('SYSTEM')} a
 * secas — la unica salida que admite {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 */
@Observed(name = "accounting.export.list")
@Service
public class ListAccountingExportsService implements ListAccountingExportsUseCase {

    private final AccountingExportRepository repository;

    public ListAccountingExportsService(AccountingExportRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<AccountingExportDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(AccountingExportDto::from);
    }

    @Override
    public PageResult<AccountingExportDto> listByPeriod(String periodKey, int page, int pageSize) {
        return repository.findAllByPeriodKey(periodKey, page, pageSize)
                .map(AccountingExportDto::from);
    }
}
