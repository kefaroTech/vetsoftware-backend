package com.vetsoftware.app.membershipsubmodule.infrastructure.web.response;

import java.time.LocalDateTime;

public record MembershipSubModuleResponse(Long id, Long membershipId, Long subModuleId, LocalDateTime createdDate) {}
