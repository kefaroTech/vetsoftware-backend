package com.vetsoftware.app.infrastructure.token;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas MySQL acotadas para no mantener bloqueos largos durante la purga.
 */
@Repository
class TokenCleanupRepository {

    static final String PURGE_REFRESH = """
            DELETE FROM refresh_tokens
            WHERE revoked_at < ? OR expires_at < ?
            ORDER BY id
            LIMIT ?
            """;
    static final String PURGE_EMAIL_VERIFICATION = """
            DELETE FROM email_verification_tokens
            WHERE consumed_at < ? OR expires_at < ?
            ORDER BY id
            LIMIT ?
            """;
    static final String PURGE_PASSWORD_RESET = """
            DELETE FROM password_reset_tokens
            WHERE consumed_at < ? OR expires_at < ?
            ORDER BY id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    TokenCleanupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    int purgeRefreshTokens(LocalDateTime cutoff, int batchSize) {
        return jdbcTemplate.update(PURGE_REFRESH, cutoff, cutoff, batchSize);
    }

    int purgeEmailVerificationTokens(LocalDateTime cutoff, int batchSize) {
        return jdbcTemplate.update(PURGE_EMAIL_VERIFICATION, cutoff, cutoff, batchSize);
    }

    int purgePasswordResetTokens(LocalDateTime cutoff, int batchSize) {
        return jdbcTemplate.update(PURGE_PASSWORD_RESET, cutoff, cutoff, batchSize);
    }

    TokenCounts countRows() {
        return new TokenCounts(count("refresh_tokens"), count("email_verification_tokens"),
                count("password_reset_tokens"));
    }

    private long count(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count == null ? 0 : count;
    }

    record TokenCounts(long refresh, long emailVerification, long passwordReset) {
        long total() {
            return refresh + emailVerification + passwordReset;
        }

        boolean anyAbove(long threshold) {
            return refresh > threshold || emailVerification > threshold
                    || passwordReset > threshold;
        }
    }
}
