package com.vetsoftware.app.employeerole.application.command;

public record UpdateEmployeeRoleCommand(Long id, Long employeeId, Long roleId) {
}
