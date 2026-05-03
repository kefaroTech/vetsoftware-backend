package com.vetsoftware.app.testtype.application.dto;

import com.vetsoftware.app.testtype.domain.TestType;
import java.time.LocalDateTime;

public record TestTypeDto(Long id, String name, String description, LocalDateTime createdDate) {
    public static TestTypeDto from(TestType testType) {
        return new TestTypeDto(
                testType.getId(),
                testType.getName(),
                testType.getDescription(),
                testType.getCreatedDate());
    }
}
