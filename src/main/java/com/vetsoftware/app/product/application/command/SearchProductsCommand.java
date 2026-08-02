package com.vetsoftware.app.product.application.command;

public record SearchProductsCommand(
    Long companyId,
    String name,
    String code,
    Long productCategoryId,
    Long taxId,
    int page,
    int pageSize) {}
