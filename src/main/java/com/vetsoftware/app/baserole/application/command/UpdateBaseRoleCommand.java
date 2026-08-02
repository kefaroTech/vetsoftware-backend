package com.vetsoftware.app.baserole.application.command;

public record UpdateBaseRoleCommand(Long id, String name, String code, Boolean mandatory) {
}
