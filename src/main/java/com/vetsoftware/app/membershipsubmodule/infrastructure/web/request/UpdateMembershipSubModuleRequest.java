package com.vetsoftware.app.membershipsubmodule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMembershipSubModuleRequest(
        @NotNull(message = "Debes seleccionar la membresía.") Long membershipId,
        @NotNull(message = "Debes seleccionar el submódulo.") Long subModuleId) {
}
