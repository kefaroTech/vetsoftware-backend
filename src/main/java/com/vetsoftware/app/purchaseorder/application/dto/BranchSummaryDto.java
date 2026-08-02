package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.purchaseorder.domain.BranchRef;

public record BranchSummaryDto(Long id, String name) {
  public static BranchSummaryDto from(BranchRef branch) {
    return new BranchSummaryDto(branch.id(), branch.name());
  }
}
