package com.vetsoftware.app.company.infrastructure.web.request;

public record CreateCompanyRequest(String name, String identifier, String address, String contactNumber) {}
