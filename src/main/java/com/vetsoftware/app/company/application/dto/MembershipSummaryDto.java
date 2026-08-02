package com.vetsoftware.app.company.application.dto;

import com.vetsoftware.app.company.domain.MembershipRef;

public record MembershipSummaryDto(Long id, String name, String status) {
  public static MembershipSummaryDto from(MembershipRef ref) {
    return new MembershipSummaryDto(ref.id(), ref.name(), ref.status());
  }
}
