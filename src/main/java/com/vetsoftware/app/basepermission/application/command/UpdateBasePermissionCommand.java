package com.vetsoftware.app.basepermission.application.command;

public record UpdateBasePermissionCommand(Long id, String name, String code, Long subModuleId) {}
