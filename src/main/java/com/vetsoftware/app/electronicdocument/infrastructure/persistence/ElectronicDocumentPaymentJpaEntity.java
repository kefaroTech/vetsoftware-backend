package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "electronic_document_payments")
public class ElectronicDocumentPaymentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "electronic_document_id", nullable = false)
    private ElectronicDocumentJpaEntity document;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_means", nullable = false, length = 30)
    private PaymentMeans paymentMeans;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected ElectronicDocumentPaymentJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ElectronicDocumentJpaEntity getDocument() {
        return document;
    }

    public void setDocument(ElectronicDocumentJpaEntity document) {
        this.document = document;
    }

    public PaymentMeans getPaymentMeans() {
        return paymentMeans;
    }

    public void setPaymentMeans(PaymentMeans paymentMeans) {
        this.paymentMeans = paymentMeans;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
