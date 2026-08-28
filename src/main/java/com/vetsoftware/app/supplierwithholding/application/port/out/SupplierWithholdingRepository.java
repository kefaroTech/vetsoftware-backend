package com.vetsoftware.app.supplierwithholding.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import java.util.Optional;

/**
 * <strong>Ningun metodo recibe {@code companyId}</strong>:
 * {@code supplier_withholdings} no tiene esa columna. La retencion la practica
 * VetSoftware.
 */
public interface SupplierWithholdingRepository {

    SupplierWithholding save(SupplierWithholding withholding);

    Optional<SupplierWithholding> findById(Long id);

    PageResult<SupplierWithholding> findAll(int page, int pageSize);

    /**
     * <strong>Barrido de plataforma</strong>: armar la declaracion del mes. Sirve a
     * {@code ix_sw_declaration (fiscal_period_key, withholding_type)}, que no lleva
     * empresa delante porque la tabla no la tiene.
     */
    PageResult<SupplierWithholding> findAllByFiscalPeriodKey(String fiscalPeriodKey, int page,
            int pageSize);

    /**
     * Lo retenido a un proveedor en un año gravable: es el certificado anual que
     * hay que entregarle. Sirve a {@code ix_sw_certificate (supplier_tax_id,
     * fiscal_year)}.
     */
    PageResult<SupplierWithholding> findAllBySupplierTaxIdAndFiscalYear(String supplierTaxId,
            int fiscalYear, int page, int pageSize);
}
