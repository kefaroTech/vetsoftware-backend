package com.vetsoftware.app.goodsreceipt.application.dto;

import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;

public record SupplierSummaryDto(Long id, String name) {
    public static SupplierSummaryDto from(SupplierRef supplier) {
        return new SupplierSummaryDto(supplier.id(), supplier.name());
    }
}
