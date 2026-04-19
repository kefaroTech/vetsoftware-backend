package com.vetsoftware.app.employee.application.command;

public record CreateEmployeeCommand(String employeeCode, String password, String name, String email,
                                    String status, Long companyId, Long createdBy) {}
