package com.vetsoftware.app.surgery.application.dto;

import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;

public record SurgeryTypeSummaryDto(Long id, String name) {
    public static SurgeryTypeSummaryDto from(SurgeryTypeRef ref) {
        return new SurgeryTypeSummaryDto(ref.id(), ref.name());
    }
}
