package com.vetsoftware.app.membership.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMembershipRequest(@NotBlank @Size(max = 100) String name,
        @NotBlank String status, boolean mandatory) {
}
