package com.vetsoftware.app.company.application.command;

public record CreateCompanyCommand(String name, String identifier, String address,
        String contactNumber, Long cityId) {
}
