package com.vetsoftware.app.membership.infrastructure.web.request;

import java.util.List;

public record CreateMembershipRequest(String name, String status, List<Long> moduleIds) {}
