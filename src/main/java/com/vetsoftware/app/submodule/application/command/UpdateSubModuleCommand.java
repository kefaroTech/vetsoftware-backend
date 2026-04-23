package com.vetsoftware.app.submodule.application.command;

public record UpdateSubModuleCommand(Long id, String name, String code, Long moduleId) {}
