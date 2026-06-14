package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ElectronicDocumentJpaMapper {

    public ElectronicDocumentJpaEntity toJpa(ElectronicDocument doc, CompanyJpaEntity company) {
        ElectronicDocumentJpaEntity entity = new ElectronicDocumentJpaEntity();
        entity.setId(doc.getId());
        entity.setCompany(company);
        entity.setOpenAccountId(doc.getOpenAccountId());
        entity.setDocumentType(doc.getDocumentType());
        entity.setPrefix(doc.getPrefix());
        entity.setConsecutive(doc.getConsecutive());
        entity.setIssueDate(doc.getIssueDate());
        entity.setIssueTime(doc.getIssueTime());
        entity.setCufe(doc.getCufe());
        entity.setCude(doc.getCude());
        entity.setUuid(doc.getUuid());
        entity.setQrData(doc.getQrData());
        entity.setQrUrl(doc.getQrUrl());
        entity.setXmlSigned(doc.getXmlSigned());
        entity.setPdfRepresentation(doc.getPdfRepresentation());
        entity.setDianStatus(doc.getDianStatus());
        entity.setDianValidationDate(doc.getDianValidationDate());

        IssuerSnapshot i = doc.getIssuer();
        entity.setIssuerDocumentType(i.documentType());
        entity.setIssuerDocumentId(i.documentId());
        entity.setIssuerVerificationDigit(i.verificationDigit());
        entity.setIssuerLegalName(i.legalName());
        entity.setIssuerTaxRegime(i.taxRegime());
        entity.setIssuerEmail(i.email());

        CustomerSnapshot c = doc.getCustomer();
        entity.setCustomerDocumentType(c.documentType());
        entity.setCustomerDocumentId(c.documentId());
        entity.setCustomerVerificationDigit(c.verificationDigit());
        entity.setCustomerPersonType(c.personType());
        entity.setCustomerLegalName(c.legalName());
        entity.setCustomerName(c.name());

        entity.setLineExtensionAmount(doc.getLineExtensionAmount());
        entity.setTaxExclusiveAmount(doc.getTaxExclusiveAmount());
        entity.setTaxInclusiveAmount(doc.getTaxInclusiveAmount());
        entity.setPayableAmount(doc.getPayableAmount());
        entity.setPaymentForm(doc.getPaymentForm());
        entity.setPaymentDueDate(doc.getPaymentDueDate());
        entity.setCreatedDate(doc.getCreatedDate());
        entity.setEnabled(doc.isEnabled());

        for (ElectronicDocumentLine l : doc.getLines()) {
            ElectronicDocumentLineJpaEntity le = new ElectronicDocumentLineJpaEntity();
            le.setDocument(entity);
            le.setLineNumber(l.getLineNumber());
            le.setDescription(l.getDescription());
            le.setQuantity(l.getQuantity());
            le.setUnitMeasureCode(l.getUnitMeasureCode());
            le.setUnitPrice(l.getUnitPrice());
            le.setLineExtensionAmount(l.getLineExtensionAmount());
            le.setTaxCategory(l.getTaxCategory());
            le.setTaxScheme(l.getTaxScheme());
            le.setTaxRate(l.getTaxRate());
            le.setTaxAmount(l.getTaxAmount());
            le.setTotalAmount(l.getTotalAmount());
            entity.getLines().add(le);
        }
        for (ElectronicDocumentPayment p : doc.getPayments()) {
            ElectronicDocumentPaymentJpaEntity pe = new ElectronicDocumentPaymentJpaEntity();
            pe.setDocument(entity);
            pe.setPaymentMeans(p.getPaymentMeans());
            pe.setAmount(p.getAmount());
            entity.getPayments().add(pe);
        }
        return entity;
    }

    public ElectronicDocument toDomain(ElectronicDocumentJpaEntity entity) {
        List<ElectronicDocumentLine> lines = entity.getLines().stream()
                .sorted(Comparator.comparingInt(ElectronicDocumentLineJpaEntity::getLineNumber))
                .map(le -> new ElectronicDocumentLine(le.getId(), le.getLineNumber(), le.getDescription(),
                        le.getQuantity(), le.getUnitMeasureCode(), le.getUnitPrice(), le.getLineExtensionAmount(),
                        le.getTaxCategory(), le.getTaxScheme(), le.getTaxRate(), le.getTaxAmount(),
                        le.getTotalAmount()))
                .toList();
        List<ElectronicDocumentPayment> payments = entity.getPayments().stream()
                .map(pe -> new ElectronicDocumentPayment(pe.getId(), pe.getPaymentMeans(), pe.getAmount()))
                .toList();

        Long companyId = entity.getCompany() == null ? null : entity.getCompany().getId();
        IssuerSnapshot issuer = new IssuerSnapshot(entity.getIssuerDocumentType(), entity.getIssuerDocumentId(),
                entity.getIssuerVerificationDigit(), entity.getIssuerLegalName(), entity.getIssuerTaxRegime(),
                entity.getIssuerEmail());
        CustomerSnapshot customer = new CustomerSnapshot(entity.getCustomerDocumentType(),
                entity.getCustomerDocumentId(), entity.getCustomerVerificationDigit(),
                entity.getCustomerPersonType(), entity.getCustomerLegalName(), entity.getCustomerName());

        return new ElectronicDocument(entity.getId(), companyId, entity.getOpenAccountId(),
                entity.getDocumentType(), entity.getPrefix(), entity.getConsecutive(), entity.getIssueDate(),
                entity.getIssueTime(), entity.getCufe(), entity.getCude(), entity.getUuid(), entity.getQrData(),
                entity.getQrUrl(), entity.getXmlSigned(), entity.getPdfRepresentation(), entity.getDianStatus(),
                entity.getDianValidationDate(), issuer, customer, entity.getLineExtensionAmount(),
                entity.getTaxExclusiveAmount(), entity.getTaxInclusiveAmount(), entity.getPayableAmount(),
                entity.getPaymentForm(), entity.getPaymentDueDate(), lines, payments, entity.getCreatedDate(),
                entity.isEnabled());
    }
}
