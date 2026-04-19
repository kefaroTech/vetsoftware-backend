package com.vetsoftware.app.application.command;

public record UpdateEmployeeCommand(Long id, String employeeCode, String name, String email, String status) {}
