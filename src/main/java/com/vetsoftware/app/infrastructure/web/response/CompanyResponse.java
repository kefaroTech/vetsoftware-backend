package com.vetsoftware.app.infrastructure.web.response;

import java.time.LocalDateTime;

public record CompanyResponse(String id, String name, String identifier, String address,
                              String contactNumber, LocalDateTime createdDate, String createdBy) {}
