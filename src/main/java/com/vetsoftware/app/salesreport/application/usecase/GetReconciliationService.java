package com.vetsoftware.app.salesreport.application.usecase;

import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto;
import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto.PendingDto;
import com.vetsoftware.app.salesreport.application.port.in.GetReconciliationUseCase;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.SalesDocumentView;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "sales.report.reconciliation")
@Service
public class GetReconciliationService implements GetReconciliationUseCase {

    private final SalesDocumentQueryPort queryPort;

    public GetReconciliationService(SalesDocumentQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public ReconciliationDto get(Long companyId, LocalDate from, LocalDate to, Long branchId) {
        List<SalesDocumentView> docs = queryPort.findByCompanyAndDateRange(companyId, from, to, branchId);
        long validados = 0, rechazados = 0, contingencia = 0, pendientes = 0;
        List<PendingDto> needsAttention = new ArrayList<>();

        for (SalesDocumentView d : docs) {
            switch (d.dianStatus()) {
                case "VALIDADO" -> validados++;
                case "RECHAZADO" -> rechazados++;
                case "CONTINGENCIA" -> contingencia++;
                default -> pendientes++; // PENDIENTE u otros
            }
            if (!"VALIDADO".equals(d.dianStatus())) {
                needsAttention.add(new PendingDto(d.id(), d.documentType(), d.prefix(), d.consecutive(),
                        d.issueDate(), d.dianStatus(), d.cufe(), d.cude()));
            }
        }
        return new ReconciliationDto(from, to, docs.size(), validados, rechazados, contingencia,
                pendientes, needsAttention);
    }
}
