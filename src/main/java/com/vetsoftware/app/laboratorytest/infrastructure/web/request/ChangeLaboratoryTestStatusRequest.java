package com.vetsoftware.app.laboratorytest.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeLaboratoryTestStatusRequest(@NotBlank String status) {
}
