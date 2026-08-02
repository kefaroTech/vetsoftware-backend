package com.vetsoftware.app.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SeededPasswordRehasher implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SeededPasswordRehasher.class);

  private final JdbcTemplate jdbc;
  private final PasswordHasher hasher;

  public SeededPasswordRehasher(JdbcTemplate jdbc, PasswordHasher hasher) {
    this.jdbc = jdbc;
    this.hasher = hasher;
  }

  @Override
  public void run(ApplicationArguments args) {
    rehashPlainTextPasswords("system_users");
    rehashPlainTextPasswords("employees");
  }

  private void rehashPlainTextPasswords(String table) {
    var rows = jdbc.queryForList("SELECT id, hash_password FROM " + table);
    for (var row : rows) {
      String current = (String) row.get("hash_password");
      if (current == null || isBcrypt(current)) continue;
      String rehashed = hasher.hash(current);
      jdbc.update(
          "UPDATE " + table + " SET hash_password = ? WHERE id = ?", rehashed, row.get("id"));
      log.info("Rehashed seeded password for {} id={}", table, row.get("id"));
    }
  }

  private boolean isBcrypt(String value) {
    return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
  }
}
