package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePayment;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SupplierInvoiceJpaMapper {

    /**
     * Write path: arma la entidad reusando los proxies JPA (getReferenceById) que
     * le pasa el repositorio.
     */
    public SupplierInvoiceJpaEntity toJpa(SupplierInvoice invoice, CompanyJpaEntity company,
            BranchJpaEntity branch, SupplierJpaEntity supplier) {
        SupplierInvoiceJpaEntity entity = new SupplierInvoiceJpaEntity();
        entity.setId(invoice.getId());
        entity.setCompany(company);
        entity.setBranch(branch);
        entity.setSupplier(supplier);
        entity.setPurchaseOrderId(invoice.getPurchaseOrderId());
        entity.setGoodsReceiptId(invoice.getGoodsReceiptId());
        entity.setInvoiceNumber(invoice.getInvoiceNumber());
        entity.setIssueDate(invoice.getIssueDate());
        entity.setDueDate(invoice.getDueDate());
        entity.setSubtotal(invoice.getSubtotal());
        entity.setTaxAmount(invoice.getTaxAmount());
        entity.setWithholdingAmount(invoice.getWithholdingAmount());
        entity.setTotal(invoice.getTotal());
        entity.setStatus(invoice.getStatus());
        entity.setNotes(invoice.getNotes());
        entity.setCreatedDate(invoice.getCreatedDate());
        entity.setCreatedBy(invoice.getCreatedBy());
        entity.setUpdatedDate(invoice.getUpdatedDate());
        entity.setUpdatedBy(invoice.getUpdatedBy());
        entity.setVersion(invoice.getVersion());
        entity.setEnabled(invoice.isEnabled());
        for (SupplierInvoicePayment p : invoice.getPayments()) {
            SupplierInvoicePaymentJpaEntity pe = new SupplierInvoicePaymentJpaEntity();
            pe.setId(p.getId());
            pe.setAmount(p.getAmount());
            pe.setPaymentDate(p.getPaymentDate());
            pe.setMethod(p.getMethod());
            pe.setReference(p.getReference());
            pe.setNote(p.getNote());
            pe.setCreatedDate(p.getCreatedDate());
            pe.setCreatedBy(p.getCreatedBy());
            entity.addPayment(pe);
        }
        return entity;
    }

    /** Read path: el @EntityGraph ya hidrató company/branch/supplier/payments. */
    public SupplierInvoice toDomain(SupplierInvoiceJpaEntity e) {
        CompanyJpaEntity c = e.getCompany();
        BranchJpaEntity b = e.getBranch();
        SupplierJpaEntity s = e.getSupplier();
        List<SupplierInvoicePayment> payments = e.getPayments().stream()
                .sorted(java.util.Comparator.comparing(SupplierInvoicePaymentJpaEntity::getId,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(pe -> new SupplierInvoicePayment(pe.getId(), pe.getAmount(),
                        pe.getPaymentDate(), pe.getMethod(), pe.getReference(), pe.getNote(),
                        pe.getCreatedDate(), pe.getCreatedBy()))
                .toList();
        return new SupplierInvoice(e.getId(),
                new CompanyRef(c.getId(), c.getName(), c.getIdentifier()),
                new BranchRef(b.getId(), b.getName()),
                new SupplierRef(s.getId(), s.getName(), s.getTaxId()), e.getPurchaseOrderId(),
                e.getGoodsReceiptId(), e.getInvoiceNumber(), e.getIssueDate(), e.getDueDate(),
                e.getSubtotal(), e.getTaxAmount(), e.getWithholdingAmount(), e.getStatus(),
                e.getNotes(), payments, e.getCreatedDate(), e.getCreatedBy(), e.getUpdatedDate(),
                e.getUpdatedBy(), e.getVersion(), e.isEnabled());
    }

    /**
     * Write-return path: rebuild del dominio reusando los Ref precargados del
     * agregado original (evita hidratar los proxies getReferenceById), tomando del
     * entity persistido los ids generados (factura + abonos, en orden).
     */
    public SupplierInvoice toDomainReusingRefs(SupplierInvoiceJpaEntity saved,
            SupplierInvoice original) {
        List<SupplierInvoicePaymentJpaEntity> savedPayments = saved.getPayments();
        List<SupplierInvoicePayment> originalPayments = original.getPayments();
        List<SupplierInvoicePayment> payments = new java.util.ArrayList<>(savedPayments.size());
        for (int i = 0; i < savedPayments.size(); i++) {
            SupplierInvoicePayment op = originalPayments.get(i);
            payments.add(new SupplierInvoicePayment(savedPayments.get(i).getId(), op.getAmount(),
                    op.getPaymentDate(), op.getMethod(), op.getReference(), op.getNote(),
                    op.getCreatedDate(), op.getCreatedBy()));
        }
        return new SupplierInvoice(saved.getId(), original.getCompany(), original.getBranch(),
                original.getSupplier(), original.getPurchaseOrderId(), original.getGoodsReceiptId(),
                original.getInvoiceNumber(), original.getIssueDate(), original.getDueDate(),
                original.getSubtotal(), original.getTaxAmount(), original.getWithholdingAmount(),
                original.getStatus(), original.getNotes(), payments, saved.getCreatedDate(),
                original.getCreatedBy(), saved.getUpdatedDate(), original.getUpdatedBy(),
                saved.getVersion(), saved.isEnabled());
    }
}
