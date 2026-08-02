package com.vetsoftware.app.laboratorytestfile.application.dto;

public record LaboratoryTestFileDownloadDto(String fileName, String contentType, byte[] content) {}
