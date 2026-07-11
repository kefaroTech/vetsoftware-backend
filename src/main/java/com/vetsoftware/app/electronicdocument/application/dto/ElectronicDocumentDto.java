package com.vetsoftware.app.electronicdocument.application.dto;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Salida de la feature. Incluye lineas, pagos y el desglose de impuesto por tarifa (calculado de las lineas). */
public record ElectronicDocumentDto(
        Long id,
        Long companyId,
        Long openAccountId,
        Long branchId,
        ElectronicDocumentType documentType,
        String prefix,
        Long consecutive,
        LocalDate issueDate,
        String issueTime,
        String cufe,
        String cude,
        String uuid,
        String qrUrl,
        DianStatus dianStatus,
        LocalDateTime dianValidationDate,
        IssuerDto issuer,
        CustomerDto customer,
        BigDecimal lineExtensionAmount,
        BigDecimal taxExclusiveAmount,
        BigDecimal taxInclusiveAmount,
        BigDecimal payableAmount,
        BigDecimal reteFuenteAmount,
        BigDecimal reteIvaAmount,
        BigDecimal reteIcaAmount,
        BigDecimal netPayableAmount,
        PaymentForm paymentForm,
        List<LineDto> lines,
        List<PaymentDto> payments,
        List<TaxTotalDto> taxTotalsByRate,
        ReferenceDto reference,
        String noteReasonCode,
        String noteReasonText,
        boolean reversed,
        LocalDateTime createdDate,
        boolean enabled
) {
    public record IssuerDto(String documentType, String documentId, String verificationDigit,
                            String legalName, String taxRegime, String email) {}

    /** Referencia al documento corregido (solo en notas credito/debito). */
    public record ReferenceDto(String cufe, String prefix, Long number, LocalDate issueDate) {}

    public record CustomerDto(String documentType, String documentId, String verificationDigit,
                              String personType, String legalName, String name, String email) {}

    public record LineDto(Long id, int lineNumber, String description, BigDecimal quantity,
                          String unitMeasureCode, BigDecimal unitPrice, BigDecimal lineExtensionAmount,
                          TaxCategory taxCategory, TaxScheme taxScheme, BigDecimal taxRate,
                          BigDecimal taxAmount, BigDecimal totalAmount) {}

    public record PaymentDto(Long id, PaymentMeans paymentMeans, String dianCode, BigDecimal amount) {}

    public record TaxTotalDto(TaxScheme taxScheme, BigDecimal taxRate,
                              BigDecimal taxableAmount, BigDecimal taxAmount) {}

    public static ElectronicDocumentDto from(ElectronicDocument doc) {
        List<LineDto> lines = doc.getLines().stream().map(ElectronicDocumentDto::lineFrom).toList();
        List<PaymentDto> payments = doc.getPayments().stream().map(ElectronicDocumentDto::paymentFrom).toList();
        return new ElectronicDocumentDto(
                doc.getId(), doc.getCompanyId(), doc.getOpenAccountId(), doc.getBranchId(), doc.getDocumentType(),
                doc.getPrefix(), doc.getConsecutive(), doc.getIssueDate(), doc.getIssueTime(),
                doc.getCufe(), doc.getCude(), doc.getUuid(), doc.getQrUrl(),
                doc.getDianStatus(), doc.getDianValidationDate(),
                issuerFrom(doc), customerFrom(doc),
                doc.getLineExtensionAmount(), doc.getTaxExclusiveAmount(),
                doc.getTaxInclusiveAmount(), doc.getPayableAmount(),
                doc.getReteFuenteAmount(), doc.getReteIvaAmount(), doc.getReteIcaAmount(),
                doc.getNetPayableAmount(),
                doc.getPaymentForm(),
                lines, payments, taxTotals(doc.getLines()),
                referenceFrom(doc), doc.getNoteReasonCode(), doc.getNoteReasonText(), doc.isReversed(),
                doc.getCreatedDate(), doc.isEnabled());
    }

    private static ReferenceDto referenceFrom(ElectronicDocument doc) {
        var r = doc.getReference();
        return r == null ? null : new ReferenceDto(r.cufe(), r.prefix(), r.number(), r.issueDate());
    }

    private static IssuerDto issuerFrom(ElectronicDocument doc) {
        var i = doc.getIssuer();
        return new IssuerDto(i.documentType(), i.documentId(), i.verificationDigit(),
                i.legalName(), i.taxRegime(), i.email());
    }

    private static CustomerDto customerFrom(ElectronicDocument doc) {
        var c = doc.getCustomer();
        return new CustomerDto(c.documentType(), c.documentId(), c.verificationDigit(),
                c.personType(), c.legalName(), c.name(), c.email());
    }

    private static LineDto lineFrom(ElectronicDocumentLine l) {
        return new LineDto(l.getId(), l.getLineNumber(), l.getDescription(), l.getQuantity(),
                l.getUnitMeasureCode(), l.getUnitPrice(), l.getLineExtensionAmount(),
                l.getTaxCategory(), l.getTaxScheme(), l.getTaxRate(), l.getTaxAmount(), l.getTotalAmount());
    }

    private static PaymentDto paymentFrom(ElectronicDocumentPayment p) {
        return new PaymentDto(p.getId(), p.getPaymentMeans(), p.getPaymentMeans().dianCode(), p.getAmount());
    }

    /** Agrupa las lineas tributadas por (esquema, tarifa) y suma base e impuesto: insumo del formulario 300. */
    private static List<TaxTotalDto> taxTotals(List<ElectronicDocumentLine> lines) {
        Map<String, TaxTotalDto> grouped = new LinkedHashMap<>();
        for (ElectronicDocumentLine l : lines) {
            if (l.getTaxScheme() == null) continue;
            String key = l.getTaxScheme().name() + "@" + l.getTaxRate();
            TaxTotalDto current = grouped.get(key);
            BigDecimal base = current == null ? BigDecimal.ZERO : current.taxableAmount();
            BigDecimal tax = current == null ? BigDecimal.ZERO : current.taxAmount();
            grouped.put(key, new TaxTotalDto(l.getTaxScheme(), l.getTaxRate(),
                    base.add(l.getLineExtensionAmount()), tax.add(l.getTaxAmount())));
        }
        return new ArrayList<>(grouped.values());
    }
}
