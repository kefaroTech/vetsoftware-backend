package com.vetsoftware.app.submodule.infrastructure.web.response;

import java.time.LocalDateTime;

public record SubModuleResponse(Long id, String name, String code, Long moduleId, LocalDateTime createdDate) {}
