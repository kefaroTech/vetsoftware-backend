package com.vetsoftware.app.laboratorytesttype.application.command;

public record UpdateLaboratoryTestTypeCommand(Long id, String name, String description, Long companyId, boolean general) {}
