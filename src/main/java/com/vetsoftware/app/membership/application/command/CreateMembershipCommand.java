package com.vetsoftware.app.membership.application.command;

public record CreateMembershipCommand(String name, String status, boolean mandatory) {
}
