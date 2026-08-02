package com.vetsoftware.app.productcategory.application.command;

public record UpdateProductCategoryCommand(
    Long id, String name, String description, Long companyId, Long updatedBy, Long version) {}
