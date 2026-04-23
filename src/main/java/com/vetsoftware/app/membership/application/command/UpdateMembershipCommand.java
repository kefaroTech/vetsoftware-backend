package com.vetsoftware.app.membership.application.command;

import java.util.List;

public record UpdateMembershipCommand(Long id, String name, String status, List<Long> moduleIds) {}
