package com.vetsoftware.app.baserole.application.command;

public record CreateBaseRoleCommand(String name, String code, Boolean mandatory) {
}
