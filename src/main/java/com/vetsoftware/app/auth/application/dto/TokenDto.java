package com.vetsoftware.app.auth.application.dto;

public record TokenDto(String token, AuthSubjectType type, String refreshToken) {
}
