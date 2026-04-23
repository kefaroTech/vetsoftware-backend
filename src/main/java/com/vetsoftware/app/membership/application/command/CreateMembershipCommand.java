package com.vetsoftware.app.membership.application.command;

import java.util.List;

public record CreateMembershipCommand(String name, String status, List<Long> moduleIds) {}
