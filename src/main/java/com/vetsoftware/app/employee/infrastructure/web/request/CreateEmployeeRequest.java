package com.vetsoftware.app.employee.infrastructure.web.request;

public record CreateEmployeeRequest(String employeeCode, String password, String name, String email,
                                    String status, Long companyId, Long createdBy) {}
