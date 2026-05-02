package com.vetsoftware.app.role.application.command;

public record CreateRoleCommand(String name, String code, Long companyId) {}
