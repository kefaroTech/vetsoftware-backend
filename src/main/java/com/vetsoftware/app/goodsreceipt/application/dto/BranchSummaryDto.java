package com.vetsoftware.app.goodsreceipt.application.dto;

import com.vetsoftware.app.goodsreceipt.domain.BranchRef;

public record BranchSummaryDto(Long id, String name) {
  public static BranchSummaryDto from(BranchRef branch) {
    return new BranchSummaryDto(branch.id(), branch.name());
  }
}
