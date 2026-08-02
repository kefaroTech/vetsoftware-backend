package com.vetsoftware.app.branch.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 30) String code,
    @Size(max = 255) String address,
    @Size(max = 30) String phone,
    @NotNull Long cityId) {}
