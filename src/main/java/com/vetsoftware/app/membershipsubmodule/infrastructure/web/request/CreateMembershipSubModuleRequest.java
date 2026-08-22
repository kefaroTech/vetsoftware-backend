package com.vetsoftware.app.membershipsubmodule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateMembershipSubModuleRequest(
        @NotNull(message = "Debes seleccionar la membresía.") Long membershipId,
        @NotNull(message = "Debes seleccionar el submódulo.") Long subModuleId) {
}
