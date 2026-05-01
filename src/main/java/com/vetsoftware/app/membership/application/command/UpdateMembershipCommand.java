package com.vetsoftware.app.membership.application.command;

public record UpdateMembershipCommand(Long id, String name, String status) {}
