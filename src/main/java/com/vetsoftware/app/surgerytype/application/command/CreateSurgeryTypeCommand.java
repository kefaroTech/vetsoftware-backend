package com.vetsoftware.app.surgerytype.application.command;

public record CreateSurgeryTypeCommand(String name, String description, Long companyId, boolean general) {}
