package com.vetsoftware.app.membership.infrastructure.web.request;

import java.util.List;

public record UpdateMembershipRequest(String name, String status, List<Long> moduleIds) {}
