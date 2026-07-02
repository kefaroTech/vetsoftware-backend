package com.vetsoftware.app.auth.application.dto;

public record TokenDto(String token, String type, String refreshToken) {}
