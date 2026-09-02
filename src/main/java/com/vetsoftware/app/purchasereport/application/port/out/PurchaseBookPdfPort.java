package com.vetsoftware.app.purchasereport.application.port.out;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;

public interface PurchaseBookPdfPort {
    byte[] renderPurchaseBook(PurchaseBookDto book);
}
