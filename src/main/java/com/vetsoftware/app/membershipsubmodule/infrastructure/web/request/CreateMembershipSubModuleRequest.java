package com.vetsoftware.app.membershipsubmodule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateMembershipSubModuleRequest(
    @NotNull Long membershipId, @NotNull Long subModuleId) {}
