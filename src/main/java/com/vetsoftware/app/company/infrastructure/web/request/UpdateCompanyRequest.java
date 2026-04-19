package com.vetsoftware.app.company.infrastructure.web.request;

public record UpdateCompanyRequest(String name, String identifier, String address, String contactNumber) {}
