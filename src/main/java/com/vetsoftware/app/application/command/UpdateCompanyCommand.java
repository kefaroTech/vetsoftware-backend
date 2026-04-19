package com.vetsoftware.app.application.command;

public record UpdateCompanyCommand(String id, String name, String identifier, String address, String contactNumber) {}
