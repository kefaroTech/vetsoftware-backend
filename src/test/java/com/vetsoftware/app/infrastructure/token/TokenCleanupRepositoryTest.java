package com.vetsoftware.app.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TokenCleanupRepositoryTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TokenCleanupRepository repository = new TokenCleanupRepository(jdbcTemplate);

    @Test
    void sendsTheSameRetentionCutoffAndBatchLimitToEveryTokenTable() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 7, 25, 12, 0);
        when(jdbcTemplate.update(TokenCleanupRepository.PURGE_REFRESH, cutoff, cutoff, 500))
                .thenReturn(4);
        when(jdbcTemplate.update(TokenCleanupRepository.PURGE_EMAIL_VERIFICATION, cutoff, cutoff, 500))
                .thenReturn(3);
        when(jdbcTemplate.update(TokenCleanupRepository.PURGE_PASSWORD_RESET, cutoff, cutoff, 500))
                .thenReturn(2);

        assertThat(repository.purgeRefreshTokens(cutoff, 500)).isEqualTo(4);
        assertThat(repository.purgeEmailVerificationTokens(cutoff, 500)).isEqualTo(3);
        assertThat(repository.purgePasswordResetTokens(cutoff, 500)).isEqualTo(2);
    }

    @Test
    void countsAllThreeTablesForGrowthMonitoring() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Long.class))
                .thenReturn(10L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_verification_tokens", Long.class))
                .thenReturn(20L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_reset_tokens", Long.class))
                .thenReturn(30L);

        TokenCleanupRepository.TokenCounts counts = repository.countRows();

        assertThat(counts.total()).isEqualTo(60);
        verify(jdbcTemplate).queryForObject("SELECT COUNT(*) FROM refresh_tokens", Long.class);
        verify(jdbcTemplate).queryForObject("SELECT COUNT(*) FROM email_verification_tokens", Long.class);
        verify(jdbcTemplate).queryForObject("SELECT COUNT(*) FROM password_reset_tokens", Long.class);
    }
}
