package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.TestTypeRef;

public record TestTypeSummaryDto(Long id, String name) {
    public static TestTypeSummaryDto from(TestTypeRef ref) {
        return new TestTypeSummaryDto(ref.id(), ref.name());
    }
}
