package com.vetsoftware.app.infrastructure.web.request;

public record UpdateCompanyRequest(String name, String identifier, String address, String contactNumber) {}
