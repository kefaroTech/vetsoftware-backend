package com.vetsoftware.app.membershipmodule.infrastructure.web.response;

import java.time.LocalDateTime;

public record MembershipModuleResponse(Long id, Long membershipId, Long subModuleId, LocalDateTime createdDate) {}
