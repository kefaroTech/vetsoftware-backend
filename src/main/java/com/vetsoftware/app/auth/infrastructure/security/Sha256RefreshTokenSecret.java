package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.port.out.RefreshTokenSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class Sha256RefreshTokenSecret implements RefreshTokenSecret {

  private static final int RAW_BYTES = 32; // 256 bits de entropía → 64 chars hex
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String generateRaw() {
    byte[] bytes = new byte[RAW_BYTES];
    secureRandom.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  @Override
  public String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no disponible", e);
    }
  }
}
