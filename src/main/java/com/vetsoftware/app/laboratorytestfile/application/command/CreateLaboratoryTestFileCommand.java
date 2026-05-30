package com.vetsoftware.app.laboratorytestfile.application.command;

public record CreateLaboratoryTestFileCommand(
        Long laboratoryTestId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        byte[] content,
        Long uploadedById
) {}
