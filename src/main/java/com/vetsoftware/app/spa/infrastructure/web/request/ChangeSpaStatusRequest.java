package com.vetsoftware.app.spa.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeSpaStatusRequest(@NotBlank String status) {
}
