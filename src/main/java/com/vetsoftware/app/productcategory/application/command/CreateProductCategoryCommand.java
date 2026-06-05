package com.vetsoftware.app.productcategory.application.command;

public record CreateProductCategoryCommand(String name, String description, Long companyId) {}
