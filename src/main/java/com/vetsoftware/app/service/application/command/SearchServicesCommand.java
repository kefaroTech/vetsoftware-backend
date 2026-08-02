package com.vetsoftware.app.service.application.command;

public record SearchServicesCommand(
    Long companyId, String name, Long serviceCategoryId, Long taxId, int page, int pageSize) {}
