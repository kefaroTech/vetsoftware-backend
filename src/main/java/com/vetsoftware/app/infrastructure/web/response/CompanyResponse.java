package com.vetsoftware.app.infrastructure.web.response;

import java.time.LocalDateTime;

public record CompanyResponse(Long id, String name, String identifier, String address,
                              String contactNumber, LocalDateTime createdDate, Long createdBy) {}
