package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordResetTokenJpaMapper — ida y vuelta dominio <-> entidad")
class PasswordResetTokenJpaMapperTest {

    private final PasswordResetTokenJpaMapper mapper = new PasswordResetTokenJpaMapper();

    @Test
    @DisplayName("toJpa conserva cada campo, incluidos id y consumedAt")
    void to_jpa_conserva_cada_campo() {
        LocalDateTime expira = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime consumido = LocalDateTime.of(2026, 1, 20, 8, 0);
        PasswordResetToken token = new PasswordResetToken(1L, 500L, 9L, "a".repeat(64), expira,
                consumido);

        PasswordResetTokenJpaEntity entity = mapper.toJpa(token);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getEmployeeId()).isEqualTo(500L);
        assertThat(entity.getCompanyId()).isEqualTo(9L);
        assertThat(entity.getTokenHash()).isEqualTo("a".repeat(64));
        assertThat(entity.getExpiresAt()).isEqualTo(expira);
        assertThat(entity.getConsumedAt()).isEqualTo(consumido);
    }

    @Test
    @DisplayName("toJpa de un token recien emitido deja id y consumedAt en null")
    void to_jpa_de_un_token_recien_emitido() {
        LocalDateTime expira = LocalDateTime.of(2026, 6, 1, 10, 0);
        PasswordResetToken token = PasswordResetToken.issue(500L, 9L, "a".repeat(64), expira);

        PasswordResetTokenJpaEntity entity = mapper.toJpa(token);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getConsumedAt()).isNull();
    }

    @Test
    @DisplayName("toDomain reconstruye el mismo agregado, con sus invariantes intactas")
    void to_domain_reconstruye_el_mismo_agregado() {
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.setId(2L);
        entity.setEmployeeId(500L);
        entity.setCompanyId(9L);
        entity.setTokenHash("b".repeat(64));
        LocalDateTime expira = LocalDateTime.of(2026, 6, 1, 10, 0);
        entity.setExpiresAt(expira);
        entity.setConsumedAt(null);

        PasswordResetToken token = mapper.toDomain(entity);

        assertThat(token.getId()).isEqualTo(2L);
        assertThat(token.getEmployeeId()).isEqualTo(500L);
        assertThat(token.getCompanyId()).isEqualTo(9L);
        assertThat(token.getTokenHash()).isEqualTo("b".repeat(64));
        assertThat(token.getExpiresAt()).isEqualTo(expira);
        assertThat(token.getConsumedAt()).isNull();
    }

    @Test
    @DisplayName("un ida y vuelta completo conserva el estado consumido")
    void ida_y_vuelta_conserva_el_estado_consumido() {
        LocalDateTime expira = LocalDateTime.of(2026, 6, 1, 10, 0);
        PasswordResetToken original = new PasswordResetToken(3L, 500L, 9L, "c".repeat(64), expira,
                LocalDateTime.of(2026, 1, 20, 8, 0));

        PasswordResetToken reconstruido = mapper.toDomain(mapper.toJpa(original));

        assertThat(reconstruido.getId()).isEqualTo(original.getId());
        assertThat(reconstruido.getEmployeeId()).isEqualTo(original.getEmployeeId());
        assertThat(reconstruido.getCompanyId()).isEqualTo(original.getCompanyId());
        assertThat(reconstruido.getTokenHash()).isEqualTo(original.getTokenHash());
        assertThat(reconstruido.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(reconstruido.getConsumedAt()).isEqualTo(original.getConsumedAt());
    }
}
