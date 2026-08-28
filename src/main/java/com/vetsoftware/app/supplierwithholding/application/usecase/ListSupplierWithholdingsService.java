package com.vetsoftware.app.supplierwithholding.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.in.ListSupplierWithholdingsUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Los tres listados de retenciones practicadas.
 *
 * <p>
 * Ninguno filtra por empresa porque {@code supplier_withholdings} no tiene esa
 * columna, y por eso los tres puertos van cerrados a {@code hasRole('SYSTEM')}
 * a secas — la unica salida que admite
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 *
 * <p>
 * Los dos ultimos son los que existen por el negocio: uno arma la declaracion
 * del mes y el otro el certificado anual del proveedor.
 */
@Observed(name = "supplier.withholding.list")
@Service
public class ListSupplierWithholdingsService implements ListSupplierWithholdingsUseCase {

    private final SupplierWithholdingRepository repository;

    public ListSupplierWithholdingsService(SupplierWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SupplierWithholdingDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(SupplierWithholdingDto::from);
    }

    @Override
    public PageResult<SupplierWithholdingDto> listByFiscalPeriod(String fiscalPeriodKey, int page,
            int pageSize) {
        return repository.findAllByFiscalPeriodKey(fiscalPeriodKey, page, pageSize)
                .map(SupplierWithholdingDto::from);
    }

    @Override
    public PageResult<SupplierWithholdingDto> listBySupplierAndYear(String supplierTaxId,
            int fiscalYear, int page, int pageSize) {
        return repository
                .findAllBySupplierTaxIdAndFiscalYear(supplierTaxId, fiscalYear, page, pageSize)
                .map(SupplierWithholdingDto::from);
    }
}
