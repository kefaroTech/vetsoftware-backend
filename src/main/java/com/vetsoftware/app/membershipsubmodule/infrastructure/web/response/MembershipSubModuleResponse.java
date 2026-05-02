package com.vetsoftware.app.membershipsubmodule.infrastructure.web.response;

import java.time.LocalDateTime;

public record MembershipSubModuleResponse(Long id,
                                           MembershipSummary membership,
                                           SubModuleSummary subModule,
                                           LocalDateTime createdDate) {}
