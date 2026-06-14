package com.vetsoftware.app.salesreport.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaEntity;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaRepository;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lee los documentos electrónicos de la empresa (read-only) para los reportes de F6. Cruce permitido de
 * vertical slicing: la infraestructura de salesreport importa el JpaRepository/JpaEntity de electronicdocument.
 * Convierte enums del dominio ajeno a String en el borde para no acoplar salesreport a ese dominio.
 */
@Component
public class JpaSalesDocumentQueryPort implements SalesDocumentQueryPort {

    private final ElectronicDocumentJpaRepository documentJpaRepository;

    public JpaSalesDocumentQueryPort(ElectronicDocumentJpaRepository documentJpaRepository) {
        this.documentJpaRepository = documentJpaRepository;
    }

    @Override
    public List<SalesDocumentView> findByCompanyAndDateRange(Long companyId, LocalDate from, LocalDate to) {
        return documentJpaRepository.findByCompanyId(companyId).stream()
                .distinct()
                .filter(e -> inRange(e.getIssueDate(), from, to))
                .sorted(java.util.Comparator.comparing(ElectronicDocumentJpaEntity::getIssueDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(this::toView)
                .toList();
    }

    private SalesDocumentView toView(ElectronicDocumentJpaEntity e) {
        List<TaxLineView> taxLines = e.getLines().stream()
                .filter(l -> l.getTaxScheme() != null)
                .map(l -> new TaxLineView(l.getTaxScheme().name(), l.getTaxRate(),
                        l.getLineExtensionAmount(), l.getTaxAmount()))
                .toList();
        List<PaymentLineView> paymentLines = e.getPayments().stream()
                .map(p -> new PaymentLineView(p.getPaymentMeans().name(),
                        p.getPaymentMeans().dianCode(), p.getAmount()))
                .toList();
        return new SalesDocumentView(
                e.getId(),
                e.getDocumentType() == null ? null : e.getDocumentType().name(),
                e.getPrefix(), e.getConsecutive(), e.getIssueDate(),
                e.getCustomerDocumentId(), e.getCustomerName(),
                e.getDianStatus() == null ? null : e.getDianStatus().name(),
                e.getCufe(), e.getCude(),
                e.getLineExtensionAmount(), e.getTaxInclusiveAmount(), e.getPayableAmount(),
                taxLines, paymentLines);
    }

    private static boolean inRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return false;
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
}
