package com.vetsoftware.app.infrastructure.web.request;

public record CreateCompanyRequest(String name, String identifier, String address, String contactNumber, String createdBy) {}
