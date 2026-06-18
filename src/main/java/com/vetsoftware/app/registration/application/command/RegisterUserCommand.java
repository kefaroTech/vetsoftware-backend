package com.vetsoftware.app.registration.application.command;

public record RegisterUserCommand(
    String companyName,
    String documentType,
    String companyIdentifier,
    String companyAddress,
    String companyContactNumber,
    Long cityId,
    String employeeName,
    String employeeEmail,
    String rawPassword,
    String taxRegime,
    String fiscalEmail
) {}
