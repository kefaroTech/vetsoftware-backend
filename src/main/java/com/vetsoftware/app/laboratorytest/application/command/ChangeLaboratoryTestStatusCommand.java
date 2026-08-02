package com.vetsoftware.app.laboratorytest.application.command;

public record ChangeLaboratoryTestStatusCommand(Long id, String status, Long processedById) {}
