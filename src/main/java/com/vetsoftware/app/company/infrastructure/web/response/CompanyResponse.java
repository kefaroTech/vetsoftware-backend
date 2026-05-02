package com.vetsoftware.app.company.infrastructure.web.response;

import java.time.LocalDateTime;

public record CompanyResponse(Long id, String name, String identifier, String address,
                              String contactNumber, CitySummary city, LocalDateTime createdDate) {}
