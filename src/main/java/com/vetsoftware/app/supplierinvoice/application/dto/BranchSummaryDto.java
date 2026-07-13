package com.vetsoftware.app.supplierinvoice.application.dto;

import com.vetsoftware.app.supplierinvoice.domain.BranchRef;

public record BranchSummaryDto(Long id, String name) {
    public static BranchSummaryDto from(BranchRef ref) {
        return new BranchSummaryDto(ref.id(), ref.name());
    }
}
