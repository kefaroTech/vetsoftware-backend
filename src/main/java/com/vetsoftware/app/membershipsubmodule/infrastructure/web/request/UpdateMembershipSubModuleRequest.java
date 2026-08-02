package com.vetsoftware.app.membershipsubmodule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMembershipSubModuleRequest(
    @NotNull Long membershipId, @NotNull Long subModuleId) {}
