package com.vetsoftware.app.role.application.command;

public record UpdateRoleCommand(Long id, String name, String code, Long companyId) {}
