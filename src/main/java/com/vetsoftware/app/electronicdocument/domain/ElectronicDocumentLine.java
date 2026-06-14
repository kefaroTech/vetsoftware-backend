package com.vetsoftware.app.electronicdocument.domain;

import java.math.BigDecimal;

/**
 * Linea del documento: foto congelada de un cargo no anulado de la cuenta.
 * Discrimina su tributo (categoria + esquema codificados) y monta base/iva/total propios.
 */
public class ElectronicDocumentLine {
    private Long id;
    private final int lineNumber;
    private final String description;
    private final BigDecimal quantity;
    private final String unitMeasureCode;
    private final BigDecimal unitPrice;
    private final BigDecimal lineExtensionAmount;
    private final TaxCategory taxCategory;
    /** null cuando la categoria no genera tributo (EXENTO/EXCLUIDO). */
    private final TaxScheme taxScheme;
    /** null cuando no aplica tributo. */
    private final BigDecimal taxRate;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;

    public ElectronicDocumentLine(Long id, int lineNumber, String description, BigDecimal quantity,
                                  String unitMeasureCode, BigDecimal unitPrice, BigDecimal lineExtensionAmount,
                                  TaxCategory taxCategory, TaxScheme taxScheme, BigDecimal taxRate,
                                  BigDecimal taxAmount, BigDecimal totalAmount) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("line description is required");
        if (description.length() > 255)
            throw new IllegalArgumentException("line description must be 255 chars or less");
        if (quantity == null) throw new IllegalArgumentException("line quantity is required");
        if (unitMeasureCode == null || unitMeasureCode.isBlank())
            throw new IllegalArgumentException("line unitMeasureCode is required");
        if (unitPrice == null) throw new IllegalArgumentException("line unitPrice is required");
        if (lineExtensionAmount == null) throw new IllegalArgumentException("line lineExtensionAmount is required");
        if (taxCategory == null) throw new IllegalArgumentException("line taxCategory is required");
        if (taxAmount == null) throw new IllegalArgumentException("line taxAmount is required");
        if (totalAmount == null) throw new IllegalArgumentException("line totalAmount is required");
        this.id = id;
        this.lineNumber = lineNumber;
        this.description = description;
        this.quantity = quantity;
        this.unitMeasureCode = unitMeasureCode;
        this.unitPrice = unitPrice;
        this.lineExtensionAmount = lineExtensionAmount;
        this.taxCategory = taxCategory;
        this.taxScheme = taxScheme;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    public Long getId() { return id; }
    public int getLineNumber() { return lineNumber; }
    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnitMeasureCode() { return unitMeasureCode; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineExtensionAmount() { return lineExtensionAmount; }
    public TaxCategory getTaxCategory() { return taxCategory; }
    public TaxScheme getTaxScheme() { return taxScheme; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
