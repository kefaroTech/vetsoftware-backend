package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto;
import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto.Bucket;
import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto.SupplierRow;
import com.vetsoftware.app.supplierinvoice.application.port.in.GetAccountsPayableAgingUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el reporte de cuentas por pagar por antigüedad: toma las facturas con saldo (PENDING/PARTIAL) y reparte el
 * saldo de cada una en un tramo según los días transcurridos desde su vencimiento hasta {@code asOf}.
 */
@Observed(name = "supplier.invoice.accounts.payable.aging")
@Service
public class GetAccountsPayableAgingService implements GetAccountsPayableAgingUseCase {
    private final SupplierInvoiceRepository repository;

    public GetAccountsPayableAgingService(SupplierInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountsPayableAgingDto get(Long companyId, Long branchId, LocalDate asOf) {
        LocalDate reference = asOf != null ? asOf : LocalDate.now();
        List<SupplierInvoice> outstanding = repository.findOutstandingByCompany(companyId, branchId);

        Map<Long, Acc> bySupplier = new LinkedHashMap<>();
        Acc grand = new Acc(null, null, null);
        for (SupplierInvoice inv : outstanding) {
            BigDecimal balance = inv.balance();
            if (balance.signum() <= 0) continue;
            long daysOverdue = ChronoUnit.DAYS.between(inv.getDueDate(), reference);
            Acc acc = bySupplier.computeIfAbsent(inv.getSupplier().id(),
                k -> new Acc(inv.getSupplier().id(), inv.getSupplier().name(), inv.getSupplier().taxId()));
            acc.add(daysOverdue, balance);
            grand.add(daysOverdue, balance);
        }

        List<SupplierRow> rows = new ArrayList<>();
        for (Acc acc : bySupplier.values()) {
            rows.add(new SupplierRow(acc.supplierId, acc.supplierName, acc.taxId, acc.toBucket()));
        }
        return new AccountsPayableAgingDto(reference, rows, grand.toBucket());
    }

    /** Acumulador mutable de saldo por tramo de un proveedor (o del gran total). */
    private static final class Acc {
        final Long supplierId;
        final String supplierName;
        final String taxId;
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal d1to30 = BigDecimal.ZERO;
        BigDecimal d31to60 = BigDecimal.ZERO;
        BigDecimal d61to90 = BigDecimal.ZERO;
        BigDecimal over90 = BigDecimal.ZERO;

        Acc(Long supplierId, String supplierName, String taxId) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.taxId = taxId;
        }

        void add(long daysOverdue, BigDecimal amount) {
            if (daysOverdue <= 0) current = current.add(amount);
            else if (daysOverdue <= 30) d1to30 = d1to30.add(amount);
            else if (daysOverdue <= 60) d31to60 = d31to60.add(amount);
            else if (daysOverdue <= 90) d61to90 = d61to90.add(amount);
            else over90 = over90.add(amount);
        }

        Bucket toBucket() {
            BigDecimal total = current.add(d1to30).add(d31to60).add(d61to90).add(over90);
            return new Bucket(current, d1to30, d31to60, d61to90, over90, total);
        }
    }
}
