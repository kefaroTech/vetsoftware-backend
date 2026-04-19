package com.vetsoftware.app.application.command;

public record CreateCompanyCommand(String name, String identifier, String address, String contactNumber, String createdBy) {}
