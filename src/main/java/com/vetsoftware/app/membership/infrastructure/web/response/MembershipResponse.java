package com.vetsoftware.app.membership.infrastructure.web.response;

import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;
import java.util.List;

public record MembershipResponse(Long id, String name, MembershipStatus status, LocalDateTime createdDate, List<Long> moduleIds) {}
