package com.vetsoftware.app.laboratorytesttype.application.command;

public record CreateLaboratoryTestTypeCommand(
    String name, String description, Long companyId, boolean general) {}
