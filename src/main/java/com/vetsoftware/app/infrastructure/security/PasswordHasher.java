package com.vetsoftware.app.infrastructure.security;

public interface PasswordHasher {
  String hash(String rawPassword);

  boolean matches(String rawPassword, String storedHash);
}
