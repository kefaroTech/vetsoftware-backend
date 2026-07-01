package com.vetsoftware.app.servicecategory.application.command;

public record UpdateServiceCategoryCommand(Long id, String name, String description, Long companyId, Long updatedBy) {}
