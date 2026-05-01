package com.vetsoftware.app.membershipmodule.application.command;

public record UpdateMembershipModuleCommand(Long id, Long membershipId, Long subModuleId) {}
