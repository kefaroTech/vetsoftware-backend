package com.vetsoftware.app.employee.application.command;

public record UpdateEmployeeCommand(Long id, String employeeCode, String name, String email) {}
