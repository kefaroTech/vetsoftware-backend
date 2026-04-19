package com.vetsoftware.app.application.command;

public record UpdateCompanyCommand(Long id, String name, String identifier, String address, String contactNumber) {}
