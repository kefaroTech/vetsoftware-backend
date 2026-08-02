package com.vetsoftware.app.servicecategory.application.command;

public record CreateServiceCategoryCommand(String name, String description, Long companyId) {
}
