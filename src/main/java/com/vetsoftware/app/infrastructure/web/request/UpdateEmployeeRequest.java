package com.vetsoftware.app.infrastructure.web.request;

public record UpdateEmployeeRequest(String employeeCode, String name, String email, String status) {}
