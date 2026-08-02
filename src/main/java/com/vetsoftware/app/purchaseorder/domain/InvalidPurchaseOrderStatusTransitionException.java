package com.vetsoftware.app.purchaseorder.domain;

public class InvalidPurchaseOrderStatusTransitionException extends RuntimeException {
    public InvalidPurchaseOrderStatusTransitionException(PurchaseOrderStatus from,
            PurchaseOrderStatus to) {
        super("Invalid purchase order status transition: " + from + " -> " + to);
    }

    public InvalidPurchaseOrderStatusTransitionException(String message) {
        super(message);
    }
}
