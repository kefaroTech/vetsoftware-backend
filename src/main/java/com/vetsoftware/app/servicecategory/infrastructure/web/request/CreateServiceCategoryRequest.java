package com.vetsoftware.app.servicecategory.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceCategoryRequest(
    @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 500) String description) {}
