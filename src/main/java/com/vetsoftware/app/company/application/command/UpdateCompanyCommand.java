package com.vetsoftware.app.company.application.command;

public record UpdateCompanyCommand(Long id, String name, String identifier, String address,
                                    String contactNumber, Long cityId) {}
