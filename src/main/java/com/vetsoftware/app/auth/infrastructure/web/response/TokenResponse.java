package com.vetsoftware.app.auth.infrastructure.web.response;

public record TokenResponse(String token, String type, String refreshToken) {}
