package com.vetsoftware.app.supplierwithholding.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSupplierWithholdingsUseCase {

    /**
     * Todas, paginadas.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas porque el puerto no transporta
     * ningun {@code companyId}</strong>, la señal que examina
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}. Aqui no hay empresa que
     * transportar: la retencion la practica VetSoftware.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SupplierWithholdingDto> listAll(int page, int pageSize);

    /**
     * <strong>La declaracion del mes.</strong> Es el barrido al que sirve
     * {@code ix_sw_declaration}: lo retenido en un periodo, de todos los
     * proveedores.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SupplierWithholdingDto> listByFiscalPeriod(String fiscalPeriodKey, int page,
            int pageSize);

    /**
     * Lo retenido a un proveedor en un año: <strong>el certificado anual que hay
     * que entregarle</strong>. Sirve a {@code ix_sw_certificate}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SupplierWithholdingDto> listBySupplierAndYear(String supplierTaxId, int fiscalYear,
            int page, int pageSize);
}
