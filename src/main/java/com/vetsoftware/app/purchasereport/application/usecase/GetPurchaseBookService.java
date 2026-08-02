package com.vetsoftware.app.purchasereport.application.usecase;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto.EntryDto;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto.TotalsDto;
import com.vetsoftware.app.purchasereport.application.port.in.GetPurchaseBookUseCase;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseDocumentQueryPort;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseDocumentQueryPort.PurchaseDocumentView;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "purchase.report.purchase.book")
@Service
public class GetPurchaseBookService implements GetPurchaseBookUseCase {

    private final PurchaseDocumentQueryPort queryPort;

    public GetPurchaseBookService(PurchaseDocumentQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public PurchaseBookDto get(Long companyId, LocalDate from, LocalDate to, Long branchId) {
        List<PurchaseDocumentView> docs = queryPort.findByCompanyAndDateRange(companyId, from, to,
                branchId);

        List<EntryDto> entries = new ArrayList<>();
        BigDecimal tSubtotal = BigDecimal.ZERO;
        BigDecimal tTax = BigDecimal.ZERO;
        BigDecimal tWith = BigDecimal.ZERO;
        BigDecimal tTotal = BigDecimal.ZERO;
        BigDecimal tPaid = BigDecimal.ZERO;
        BigDecimal tBalance = BigDecimal.ZERO;

        for (PurchaseDocumentView d : docs) {
            entries.add(new EntryDto(d.id(), d.supplierName(), d.supplierTaxId(), d.invoiceNumber(),
                    d.issueDate(), d.dueDate(), nz(d.subtotal()), nz(d.taxAmount()),
                    nz(d.withholdingAmount()), nz(d.total()), nz(d.paidAmount()), nz(d.balance()),
                    d.status()));
            tSubtotal = tSubtotal.add(nz(d.subtotal()));
            tTax = tTax.add(nz(d.taxAmount()));
            tWith = tWith.add(nz(d.withholdingAmount()));
            tTotal = tTotal.add(nz(d.total()));
            tPaid = tPaid.add(nz(d.paidAmount()));
            tBalance = tBalance.add(nz(d.balance()));
        }

        TotalsDto totals = new TotalsDto(entries.size(), tSubtotal, tTax, tWith, tTotal, tPaid,
                tBalance);
        return new PurchaseBookDto(from, to, entries, totals);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
