package com.vetsoftware.app.purchasereport.application.port.out;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;

/** Renderiza el libro de compras a PDF. */
public interface PurchaseBookPdfPort {
    byte[] renderPurchaseBook(PurchaseBookDto book);
}
