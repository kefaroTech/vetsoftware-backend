package com.vetsoftware.app.employee.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendInvitationRequest(@NotBlank @Size(min = 8, max = 100) String password) {
}
