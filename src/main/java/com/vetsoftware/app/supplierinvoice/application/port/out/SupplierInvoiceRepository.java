package com.vetsoftware.app.supplierinvoice.application.port.out;

import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import java.util.List;
import java.util.Optional;

public interface SupplierInvoiceRepository {
    SupplierInvoice save(SupplierInvoice invoice);

    Optional<SupplierInvoice> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<SupplierInvoice> search(SearchSupplierInvoicesCommand command);

    /**
     * Facturas con saldo pendiente (PENDING/PARTIAL) para el aging; filtra por sede
     * si {@code
     * branchId} != null.
     */
    List<SupplierInvoice> findOutstandingByCompany(Long companyId, Long branchId);

    /**
     * ¿Ya existe una factura activa con ese número para ese proveedor en la
     * empresa? (evita doble registro).
     */
    boolean existsByCompanySupplierAndNumber(Long companyId, Long supplierId, String invoiceNumber);

    void delete(Long id);
}
