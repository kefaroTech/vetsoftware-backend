package com.vetsoftware.app.membershipsubmodule.application.command;

public record UpdateMembershipSubModuleCommand(Long id, Long membershipId, Long subModuleId) {
}
