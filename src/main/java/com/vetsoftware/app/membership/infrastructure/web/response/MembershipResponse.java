package com.vetsoftware.app.membership.infrastructure.web.response;

import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;

public record MembershipResponse(Long id, String name, MembershipStatus status, LocalDateTime createdDate, Long createdBy) {}
