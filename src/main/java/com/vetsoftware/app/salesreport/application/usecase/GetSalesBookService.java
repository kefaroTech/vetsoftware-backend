package com.vetsoftware.app.salesreport.application.usecase;

import com.vetsoftware.app.salesreport.application.dto.SalesBookDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto.EntryDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto.RecaudoDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto.TaxByRateDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto.TotalsDto;
import com.vetsoftware.app.salesreport.application.port.in.GetSalesBookUseCase;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.PaymentLineView;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.SalesDocumentView;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.TaxLineView;
import com.vetsoftware.app.salesreport.domain.ReportDateRange;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Observed(name = "sales.report.sales.book")
@Service
public class GetSalesBookService implements GetSalesBookUseCase {
    private static final String IVA = "IVA";
    private static final String INC = "INC";

    private final SalesDocumentQueryPort queryPort;

    public GetSalesBookService(SalesDocumentQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    /**
     * El periodo se valida antes de consultar nada: {@link ReportDateRange} rechaza
     * un rango invertido, que sin la invariante producia un libro con todo en cero
     * indistinguible de un periodo real sin ventas. La
     * {@code IllegalArgumentException} sale como 400 por el
     * {@code GlobalExceptionHandler}.
     */
    @Override
    public SalesBookDto get(Long companyId, LocalDate from, LocalDate to, Long branchId) {
        ReportDateRange range = new ReportDateRange(from, to);
        List<SalesDocumentView> docs = queryPort.findByCompanyAndDateRange(companyId, range.from(),
                range.to(), branchId);

        List<EntryDto> entries = new ArrayList<>();
        Map<String, TaxByRateDto> taxByRate = new LinkedHashMap<>();
        Map<String, RecaudoDto> recaudo = new LinkedHashMap<>();
        BigDecimal tBase = BigDecimal.ZERO;
        BigDecimal tIva = BigDecimal.ZERO;
        BigDecimal tInc = BigDecimal.ZERO;
        BigDecimal tTotal = BigDecimal.ZERO;
        BigDecimal tPayable = BigDecimal.ZERO;
        BigDecimal tReteFuente = BigDecimal.ZERO;
        BigDecimal tReteIva = BigDecimal.ZERO;
        BigDecimal tReteIca = BigDecimal.ZERO;

        for (SalesDocumentView d : docs) {
            BigDecimal iva = sumScheme(d.taxLines(), IVA);
            BigDecimal inc = sumScheme(d.taxLines(), INC);
            entries.add(new EntryDto(d.id(), d.documentType(), d.prefix(), d.consecutive(),
                    d.issueDate(), d.customerDocumentId(), d.customerName(),
                    d.lineExtensionAmount(), iva, inc, d.taxInclusiveAmount(), d.payableAmount(),
                    nz(d.reteFuente()), nz(d.reteIva()), nz(d.reteIca()), d.dianStatus(), d.cufe(),
                    d.cude()));

            for (TaxLineView t : d.taxLines()) {
                String key = t.taxScheme() + "@" + t.taxRate();
                TaxByRateDto cur = taxByRate.get(key);
                BigDecimal base = (cur == null ? BigDecimal.ZERO : cur.taxableAmount())
                        .add(nz(t.taxableAmount()));
                BigDecimal tax = (cur == null ? BigDecimal.ZERO : cur.taxAmount())
                        .add(nz(t.taxAmount()));
                taxByRate.put(key, new TaxByRateDto(t.taxScheme(), t.taxRate(), base, tax));
            }
            for (PaymentLineView p : d.paymentLines()) {
                String key = p.dianCode();
                RecaudoDto cur = recaudo.get(key);
                BigDecimal amount = (cur == null ? BigDecimal.ZERO : cur.amount())
                        .add(nz(p.amount()));
                recaudo.put(key, new RecaudoDto(p.paymentMeans(), p.dianCode(), amount));
            }

            tBase = tBase.add(nz(d.lineExtensionAmount()));
            tIva = tIva.add(iva);
            tInc = tInc.add(inc);
            tTotal = tTotal.add(nz(d.taxInclusiveAmount()));
            tPayable = tPayable.add(nz(d.payableAmount()));
            tReteFuente = tReteFuente.add(nz(d.reteFuente()));
            tReteIva = tReteIva.add(nz(d.reteIva()));
            tReteIca = tReteIca.add(nz(d.reteIca()));
        }

        TotalsDto totals = new TotalsDto(entries.size(), tBase, tIva, tInc, tTotal, tPayable,
                tReteFuente, tReteIva, tReteIca);
        return new SalesBookDto(from, to, entries, new ArrayList<>(taxByRate.values()),
                new ArrayList<>(recaudo.values()), totals);
    }

    private static BigDecimal sumScheme(List<TaxLineView> lines, String scheme) {
        return lines.stream().filter(l -> scheme.equals(l.taxScheme())).map(l -> nz(l.taxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
