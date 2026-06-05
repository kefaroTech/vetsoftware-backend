package com.vetsoftware.app.productcategory.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProductCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 500) String description
) {}
