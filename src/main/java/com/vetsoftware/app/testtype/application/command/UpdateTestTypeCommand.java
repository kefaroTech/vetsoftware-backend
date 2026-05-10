package com.vetsoftware.app.testtype.application.command;

public record UpdateTestTypeCommand(Long id, String name, String description, Long companyId, boolean general) {}
